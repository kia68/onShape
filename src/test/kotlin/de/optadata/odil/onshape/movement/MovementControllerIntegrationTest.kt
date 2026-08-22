package de.optadata.odil.onshape.movement

import de.optadata.odil.onshape.AbstractIntegrationTest
import de.optadata.odil.onshape.auth.AuthResponse
import de.optadata.odil.onshape.auth.RegisterRequest
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc
import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import tools.jackson.databind.ObjectMapper
import kotlin.test.assertEquals

/** FR-110/111/114. Bewusst OHNE @Transactional, siehe AuthControllerIntegrationTest-Kommentar
 * (Epic Onboarding). */
@SpringBootTest
@AutoConfigureMockMvc
class MovementControllerIntegrationTest : AbstractIntegrationTest() {

    @Autowired lateinit var mockMvc: MockMvc
    @Autowired lateinit var objectMapper: ObjectMapper

    private fun bearer(token: String) = "Bearer $token"

    private fun registerAndOnboard(experience: String, sex: String, weightKg: Double): String {
        val email = "movement-test-${System.nanoTime()}@example.test"
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
                            "sex" to sex, "birthDate" to "1995-05-01", "heightCm" to 170.0, "weightKg" to weightKg,
                            "experience" to experience, "activityPal" to 1.4, "goal" to "maintain", "goalRatePctWeek" to 0.0,
                            "dietaryPrefs" to emptyList<String>(), "allergens" to emptyList<String>(), "injuries" to emptyList<String>(),
                            "equipment" to listOf("bodyweight", "dumbbells", "barbell"), "trainingDaysWeek" to 3, "sessionMinutes" to 45,
                            "healthScreening" to mapOf("heartCondition" to false, "pregnancy" to false, "recentInjury" to false, "medication" to false),
                        ),
                    ),
                ),
        ).andExpect(status().isOk)
        return token
    }

    private fun exerciseIdBySlug(token: String, slug: String): String {
        val response = mockMvc.perform(get("/api/training/exercises").header(HttpHeaders.AUTHORIZATION, bearer(token)))
            .andExpect(status().isOk).andReturn().response.contentAsString
        val node = objectMapper.readTree(response).first { it.get("slug").asText() == slug }
        return node.get("id").asText()
    }

    @Test
    fun `uebung mit content zeigt aufbau-schritte cues und fehler, ohne empfehlung fuer koerpergewichtsuebungen`() {
        val token = registerAndOnboard(experience = "beginner", sex = "female", weightKg = 65.0)
        val exerciseId = exerciseIdBySlug(token, "bodyweight-squat")

        mockMvc.perform(get("/api/movement/exercises/$exerciseId").header(HttpHeaders.AUTHORIZATION, bearer(token)))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.hasContent").value(true))
            .andExpect(jsonPath("$.cues.length()").value(3))
            .andExpect(jsonPath("$.mistakes.length()").value(3))
            .andExpect(jsonPath("$.startingWeight.weightKg").doesNotExist())
            .andExpect(jsonPath("$.startingWeight.reasonCode").value("bodyweight_only"))
            .andExpect(jsonPath("$.showBeginnerIntro").value(true))
    }

    @Test
    fun `langhantel-uebung ohne redaktionellen content empfiehlt trotzdem die leere stange`() {
        val token = registerAndOnboard(experience = "advanced", sex = "male", weightKg = 85.0)
        val exerciseId = exerciseIdBySlug(token, "back-squat")

        mockMvc.perform(get("/api/movement/exercises/$exerciseId").header(HttpHeaders.AUTHORIZATION, bearer(token)))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.hasContent").value(false))
            .andExpect(jsonPath("$.startingWeight.weightKg").value(20.0))
            .andExpect(jsonPath("$.startingWeight.reasonCode").value("barbell_empty"))
            // fortgeschrittene Nutzer bekommen die Anfaenger-Einblendung nicht
            .andExpect(jsonPath("$.showBeginnerIntro").value(false))
    }

    @Test
    fun `anfaenger-einblendung verschwindet nach dem ersten geloggten satz`() {
        val token = registerAndOnboard(experience = "none", sex = "other", weightKg = 70.0)
        val exerciseId = exerciseIdBySlug(token, "pushup")

        mockMvc.perform(get("/api/movement/exercises/$exerciseId").header(HttpHeaders.AUTHORIZATION, bearer(token)))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.showBeginnerIntro").value(true))

        val sessionId = objectMapper.readTree(
            mockMvc.perform(
                post("/api/trainlog/sessions").header(HttpHeaders.AUTHORIZATION, bearer(token))
                    .contentType(MediaType.APPLICATION_JSON).content("{}"),
            ).andReturn().response.contentAsString,
        ).get("id").asText()
        mockMvc.perform(
            post("/api/trainlog/sessions/$sessionId/sets").header(HttpHeaders.AUTHORIZATION, bearer(token))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(mapOf("exerciseId" to exerciseId, "setIndex" to 0, "reps" to 10, "completed" to true))),
        ).andExpect(status().isCreated)

        mockMvc.perform(get("/api/movement/exercises/$exerciseId").header(HttpHeaders.AUTHORIZATION, bearer(token)))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.showBeginnerIntro").value(false))
    }

    @Test
    fun `progressionsleiter verlinkt regression und progression mit slug und name`() {
        val token = registerAndOnboard(experience = "intermediate", sex = "male", weightKg = 80.0)
        val exerciseId = exerciseIdBySlug(token, "goblet-squat")

        val response = mockMvc.perform(get("/api/movement/exercises/$exerciseId").header(HttpHeaders.AUTHORIZATION, bearer(token)))
            .andExpect(status().isOk).andReturn().response.contentAsString
        val json = objectMapper.readTree(response)
        assertEquals("bodyweight-squat", json.get("regressionOf").get("slug").asText())
        assertEquals("back-squat", json.get("progressionTo").get("slug").asText())
    }
}
