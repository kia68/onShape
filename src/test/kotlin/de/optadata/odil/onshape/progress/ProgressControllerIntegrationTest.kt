package de.optadata.odil.onshape.progress

import de.optadata.odil.onshape.AbstractIntegrationTest
import de.optadata.odil.onshape.auth.AuthResponse
import de.optadata.odil.onshape.auth.RegisterRequest
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc
import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.header
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import tools.jackson.databind.ObjectMapper
import java.util.UUID
import java.util.zip.ZipInputStream
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/** FR-130/131/133/137. Bewusst OHNE @Transactional, siehe AuthControllerIntegrationTest-Kommentar
 * (Epic Onboarding). */
@SpringBootTest
@AutoConfigureMockMvc
class ProgressControllerIntegrationTest : AbstractIntegrationTest() {

    @Autowired lateinit var mockMvc: MockMvc
    @Autowired lateinit var objectMapper: ObjectMapper
    @Autowired lateinit var jdbcTemplate: JdbcTemplate

    private fun bearer(token: String) = "Bearer $token"

    private fun registerAndOnboard(): String {
        val email = "progress-test-${System.nanoTime()}@example.test"
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

    private fun seedFood(nameDe: String, kcal: Double): UUID =
        jdbcTemplate.queryForObject(
            """
            INSERT INTO foods (source, trust, name_de, name_en, kcal, protein_g, fat_g, carbs_g)
            VALUES ('user'::food_source_t, 'estimated'::trust_t, ?, ?, ?, 10, 5, 8) RETURNING id
            """.trimIndent(),
            UUID::class.java,
            nameDe, nameDe, kcal,
        )!!

    @Test
    fun `gewichtsverlauf liefert rohwerte und gleitendes sieben-tage-mittel`() {
        val token = registerAndOnboard()
        for ((day, weight) in listOf("2026-02-01" to 80.0, "2026-02-02" to 81.0, "2026-02-03" to 79.0)) {
            mockMvc.perform(
                post("/api/measurements").header(HttpHeaders.AUTHORIZATION, bearer(token))
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(mapOf("measuredOn" to day, "weightKg" to weight))),
            ).andExpect(status().isCreated)
        }

        val response = mockMvc.perform(
            get("/api/progress/weight").param("from", "2026-01-01").param("to", "2026-12-31")
                .header(HttpHeaders.AUTHORIZATION, bearer(token)),
        ).andExpect(status().isOk).andReturn().response.contentAsString
        val json = objectMapper.readTree(response)
        // Onboarding selbst legt bereits einen ersten Messpunkt (80.0 an einem anderen Datum) an,
        // daher mindestens die drei hier gesetzten plus den Onboarding-Punkt.
        assertTrue(json.get("raw").size() >= 3)
        assertEquals(json.get("raw").size(), json.get("sevenDayAverage").size())
    }

    @Test
    fun `naehrwert-verlauf berechnet adhaerenz aus geloggten tagen`() {
        val token = registerAndOnboard()
        val foodId = seedFood("Testbrot-${System.nanoTime()}", 200.0)

        // 2 von 4 Tagen im Zeitraum geloggt
        for (day in listOf("2026-03-01", "2026-03-02")) {
            mockMvc.perform(
                post("/api/nutrition/entries").header(HttpHeaders.AUTHORIZATION, bearer(token))
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(
                        objectMapper.writeValueAsString(
                            mapOf("foodId" to foodId, "loggedDate" to day, "slot" to "breakfast", "grams" to 100.0, "method" to "search"),
                        ),
                    ),
            ).andExpect(status().isCreated)
        }

        val response = mockMvc.perform(
            get("/api/progress/nutrition").param("from", "2026-03-01").param("to", "2026-03-04")
                .header(HttpHeaders.AUTHORIZATION, bearer(token)),
        ).andExpect(status().isOk).andReturn().response.contentAsString
        val json = objectMapper.readTree(response)
        assertEquals(2, json.get("daily").size())
        assertEquals(0.5, json.get("adherenceRate").asDouble(), 0.0001)
        assertTrue(json.get("targetKcal").asInt() > 0)
    }

    @Test
    fun `volumenverlauf zeigt tatsaechlich geloggte saetze mit korridor aus dem profil`() {
        val token = registerAndOnboard()
        val exerciseId = objectMapper.readTree(
            mockMvc.perform(get("/api/training/exercises").header(HttpHeaders.AUTHORIZATION, bearer(token)))
                .andReturn().response.contentAsString,
        ).first { it.get("slug").asText() == "bodyweight-squat" }.get("id").asText()

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

        val response = mockMvc.perform(
            get("/api/progress/volume").param("from", "2020-01-01").param("to", "2030-01-01")
                .header(HttpHeaders.AUTHORIZATION, bearer(token)),
        ).andExpect(status().isOk).andReturn().response.contentAsString
        val entries = objectMapper.readTree(response)
        assertTrue(entries.size() > 0)
        val quads = entries.first { it.get("muscle").asText() == "quads" }
        assertEquals(1.0, quads.get("sets").asDouble(), 0.001)
        assertEquals(12, quads.get("corridorMin").asInt())
        assertEquals(16, quads.get("corridorMax").asInt())
    }

    @Test
    fun `json-export enthaelt profil und json-datei-header`() {
        val token = registerAndOnboard()
        mockMvc.perform(get("/api/export/json").header(HttpHeaders.AUTHORIZATION, bearer(token)))
            .andExpect(status().isOk)
            .andExpect(header().string(HttpHeaders.CONTENT_DISPOSITION, org.hamcrest.Matchers.containsString("onshape-export.json")))
            // JSON-Export serialisiert die rohen Domain-Objekte (bewusst vollstaendig, siehe
            // ExportService-KDoc) -- Enums also ueber ihren Kotlin-Namen, nicht ueber .dbValue
            // wie sonst in den *Response-DTOs* dieser App ueblich.
            .andExpect(jsonPath("$.profile.sex").value("MALE"))
    }

    @Test
    fun `csv-export liefert ein zip mit den erwarteten dateien`() {
        val token = registerAndOnboard()
        val result = mockMvc.perform(get("/api/export/csv").header(HttpHeaders.AUTHORIZATION, bearer(token)))
            .andExpect(status().isOk).andReturn()
        val entryNames = mutableListOf<String>()
        ZipInputStream(result.response.contentAsByteArray.inputStream()).use { zip ->
            generateSequence { zip.nextEntry }.forEach { entryNames.add(it.name) }
        }
        assertTrue("profile.csv" in entryNames)
        assertTrue("body_measurements.csv" in entryNames)
        assertTrue("food_entries.csv" in entryNames)
        assertTrue("workout_sets.csv" in entryNames)
        assertTrue("programs.csv" in entryNames)
    }
}
