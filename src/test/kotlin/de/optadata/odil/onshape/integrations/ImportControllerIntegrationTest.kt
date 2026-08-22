package de.optadata.odil.onshape.integrations

import de.optadata.odil.onshape.AbstractIntegrationTest
import de.optadata.odil.onshape.auth.AuthResponse
import de.optadata.odil.onshape.auth.RegisterRequest
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc
import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType
import org.springframework.mock.web.MockMultipartFile
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import tools.jackson.databind.ObjectMapper

/** FR-153. Bewusst OHNE @Transactional, siehe AuthControllerIntegrationTest-Kommentar (Epic Onboarding). */
@SpringBootTest
@AutoConfigureMockMvc
class ImportControllerIntegrationTest : AbstractIntegrationTest() {

    @Autowired lateinit var mockMvc: MockMvc
    @Autowired lateinit var objectMapper: ObjectMapper

    private fun bearer(token: String) = "Bearer $token"

    private fun registerAndOnboard(): String {
        val email = "import-test-${System.nanoTime()}@example.test"
        val registerResponse = mockMvc.perform(
            post("/api/auth/register").contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(RegisterRequest(email, "correct-horse-1", "de"))),
        ).andReturn().response.contentAsString
        val token = objectMapper.readValue(registerResponse, AuthResponse::class.java).token

        mockMvc.perform(
            put("/api/onboarding/profile").header(HttpHeaders.AUTHORIZATION, bearer(token))
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    objectMapper.writeValueAsString(
                        mapOf(
                            "sex" to "male", "birthDate" to "1995-05-01", "heightCm" to 180.0, "weightKg" to 80.0,
                            "experience" to "intermediate", "activityPal" to 1.4, "goal" to "maintain", "goalRatePctWeek" to 0.0,
                            "dietaryPrefs" to emptyList<String>(), "allergens" to emptyList<String>(), "injuries" to emptyList<String>(),
                            "equipment" to listOf("bodyweight", "dumbbells", "barbell"), "trainingDaysWeek" to 3, "sessionMinutes" to 45,
                            "healthScreening" to mapOf("heartCondition" to false, "pregnancy" to false, "recentInjury" to false, "medication" to false),
                        ),
                    ),
                ),
        ).andExpect(status().isOk)
        return token
    }

    private val hevyHeader = "\"title\",\"start_time\",\"end_time\",\"description\",\"exercise_title\",\"superset_id\",\"exercise_notes\"," +
        "\"set_index\",\"set_type\",\"weight_kg\",\"reps\",\"distance_km\",\"duration_seconds\",\"rpe\""

    @Test
    fun `hevy-import legt session und saetze an und ist beim zweiten mal idempotent`() {
        val token = registerAndOnboard()
        val csv = hevyHeader + "\n" +
            "\"Leg Day\",\"1 Jan 2026, 10:00\",\"1 Jan 2026, 10:45\",\"\",\"Back Squat\",,\"\",0,\"normal\",60,8,,0,7\n" +
            "\"Leg Day\",\"1 Jan 2026, 10:00\",\"1 Jan 2026, 10:45\",\"\",\"Back Squat\",,\"\",1,\"normal\",65,6,,0,8\n"
        val file = MockMultipartFile("file", "hevy.csv", "text/csv", csv.toByteArray(Charsets.UTF_8))

        val first = mockMvc.perform(multipart("/api/import/hevy").file(file).header(HttpHeaders.AUTHORIZATION, bearer(token)))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.sessionsImported").value(1))
            .andExpect(jsonPath("$.setsImported").value(2))
            .andExpect(jsonPath("$.unmatchedExercises").isEmpty())
            .andReturn()
        require(first.response.status == 200)

        // erneuter Upload derselben Datei: idempotent, keine neuen Zeilen
        mockMvc.perform(multipart("/api/import/hevy").file(file).header(HttpHeaders.AUTHORIZATION, bearer(token)))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.sessionsImported").value(0))
            .andExpect(jsonPath("$.setsImported").value(0))
    }

    @Test
    fun `unbekannte uebung wird transparent als unmatched gemeldet statt geraten`() {
        val token = registerAndOnboard()
        val csv = hevyHeader + "\n" +
            "\"W\",\"1 Jan 2026, 10:00\",\"1 Jan 2026, 10:45\",\"\",\"Nordic Hamstring Curl\",,\"\",0,\"normal\",0,10,,0,8\n"
        val file = MockMultipartFile("file", "hevy.csv", "text/csv", csv.toByteArray(Charsets.UTF_8))

        mockMvc.perform(multipart("/api/import/hevy").file(file).header(HttpHeaders.AUTHORIZATION, bearer(token)))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.sessionsImported").value(1))
            .andExpect(jsonPath("$.setsImported").value(0))
            .andExpect(jsonPath("$.unmatchedExercises.['Nordic Hamstring Curl']").value(1))
    }

    @Test
    fun `strong-import parst reales format und meldet gewichts-einheiten-warnung`() {
        val token = registerAndOnboard()
        val csv = "Date,Workout Name,Duration,Exercise Name,Set Order,Weight,Reps,Distance,Seconds,Notes,Workout Notes,RPE\n" +
            "2026-01-01 08:00:00,\"Push Day\",45m,\"Bench Press (Barbell)\",1,60,8,0,0,,,\n"
        val file = MockMultipartFile("file", "strong.csv", "text/csv", csv.toByteArray(Charsets.UTF_8))

        mockMvc.perform(multipart("/api/import/strong").file(file).header(HttpHeaders.AUTHORIZATION, bearer(token)))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.sessionsImported").value(1))
            .andExpect(jsonPath("$.setsImported").value(1))
            .andExpect(jsonPath("$.warnings[0]").value(org.hamcrest.Matchers.containsString("kg")))
    }

    @Test
    fun `leere datei ergibt 400`() {
        val token = registerAndOnboard()
        val file = MockMultipartFile("file", "empty.csv", "text/csv", ByteArray(0))

        mockMvc.perform(multipart("/api/import/hevy").file(file).header(HttpHeaders.AUTHORIZATION, bearer(token)))
            .andExpect(status().isBadRequest)
    }
}
