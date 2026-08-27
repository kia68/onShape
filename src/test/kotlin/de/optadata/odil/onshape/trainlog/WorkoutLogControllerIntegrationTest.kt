package de.optadata.odil.onshape.trainlog

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

/** FR-90..FR-93, FR-96..FR-98. Bewusst OHNE @Transactional, siehe AuthControllerIntegrationTest-
 * Kommentar (Epic Onboarding): RLS-Session-Variablen brauchen echte, separate, committete
 * Transaktionen pro HTTP-Request. */
@SpringBootTest
@AutoConfigureMockMvc
class WorkoutLogControllerIntegrationTest : AbstractIntegrationTest() {

    @Autowired lateinit var mockMvc: MockMvc
    @Autowired lateinit var objectMapper: ObjectMapper

    private fun bearer(token: String) = "Bearer $token"

    private fun registerAndGetToken(): String {
        val email = "trainlog-test-${System.nanoTime()}@example.test"
        val response = mockMvc.perform(
            post("/api/auth/register").contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(RegisterRequest(email, "correct-horse-1", "de"))),
        ).andReturn().response.contentAsString
        return objectMapper.readValue(response, AuthResponse::class.java).token
    }

    private fun anyExerciseId(token: String): String {
        val response = mockMvc.perform(get("/api/training/exercises").header(HttpHeaders.AUTHORIZATION, bearer(token)))
            .andExpect(status().isOk).andReturn().response.contentAsString
        return objectMapper.readTree(response).get(0).get("id").asText()
    }

    @Test
    fun `session starten satz loggen erkennt beim ersten satz alle vier pr-dimensionen`() {
        val token = registerAndGetToken()
        val exerciseId = anyExerciseId(token)

        val sessionResponse = mockMvc.perform(
            post("/api/trainlog/sessions").header(HttpHeaders.AUTHORIZATION, bearer(token))
                .contentType(MediaType.APPLICATION_JSON).content(objectMapper.writeValueAsString(mapOf("programDayId" to null, "clientId" to null))),
        ).andExpect(status().isCreated).andReturn().response.contentAsString
        val sessionId = objectMapper.readTree(sessionResponse).get("id").asText()

        mockMvc.perform(get("/api/trainlog/sessions/active").header(HttpHeaders.AUTHORIZATION, bearer(token)))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.id").value(sessionId))

        val setResponse = mockMvc.perform(
            post("/api/trainlog/sessions/$sessionId/sets").header(HttpHeaders.AUTHORIZATION, bearer(token))
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    objectMapper.writeValueAsString(
                        mapOf(
                            "exerciseId" to exerciseId, "setIndex" to 0, "weightKg" to 100.0, "reps" to 5,
                            "rir" to 2, "isWarmup" to false, "completed" to true, "clientId" to "set-1",
                        ),
                    ),
                ),
        ).andExpect(status().isCreated).andReturn().response.contentAsString
        val json = objectMapper.readTree(setResponse)
        val prTypes = mutableSetOf<String>()
        json.get("personalRecords").forEach { prTypes.add(it.get("type").asText()) }
        assertEquals(setOf("MAX_WEIGHT", "MAX_REPS_AT_WEIGHT", "EST_1RM", "VOLUME"), prTypes)
    }

    @Test
    fun `schwererer aber kuerzerer satz ist nur bei gewicht und wiederholungsdimension ein rekord`() {
        val token = registerAndGetToken()
        val exerciseId = anyExerciseId(token)
        val sessionId = objectMapper.readTree(
            mockMvc.perform(
                post("/api/trainlog/sessions").header(HttpHeaders.AUTHORIZATION, bearer(token))
                    .contentType(MediaType.APPLICATION_JSON).content("{}"),
            ).andReturn().response.contentAsString,
        ).get("id").asText()

        mockMvc.perform(
            post("/api/trainlog/sessions/$sessionId/sets").header(HttpHeaders.AUTHORIZATION, bearer(token))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(mapOf("exerciseId" to exerciseId, "setIndex" to 0, "weightKg" to 100.0, "reps" to 8))),
        ).andExpect(status().isCreated)

        // 110kg x 3 nach 100kg x 8: schwerer (Gewichts-PR), UND ein Wiederholungsrekord fuer die
        // Gewichtsklasse >=110kg (dafuer gibt es noch keine Vergleichsdaten -- jede Wdh-Zahl ist
        // dort per Definition ein Erstrekord, siehe PersonalRecordDetector-KDoc). Volumen (330 <
        // 800) und geschaetztes 1RM (110kg x 3 rechnet sich niedriger als 100kg x 8) sind dagegen
        // KEIN Rekord.
        val secondResponse = mockMvc.perform(
            post("/api/trainlog/sessions/$sessionId/sets").header(HttpHeaders.AUTHORIZATION, bearer(token))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(mapOf("exerciseId" to exerciseId, "setIndex" to 1, "weightKg" to 110.0, "reps" to 3))),
        ).andExpect(status().isCreated).andReturn().response.contentAsString
        val prTypes = mutableSetOf<String>()
        objectMapper.readTree(secondResponse).get("personalRecords").forEach { prTypes.add(it.get("type").asText()) }
        assertEquals(setOf("MAX_WEIGHT", "MAX_REPS_AT_WEIGHT"), prTypes)
    }

    @Test
    fun `vorbelegung bezieht sich auf die vorherige session nicht auf die laufende`() {
        val token = registerAndGetToken()
        val exerciseId = anyExerciseId(token)

        val firstSessionId = objectMapper.readTree(
            mockMvc.perform(
                post("/api/trainlog/sessions").header(HttpHeaders.AUTHORIZATION, bearer(token))
                    .contentType(MediaType.APPLICATION_JSON).content("{}"),
            ).andReturn().response.contentAsString,
        ).get("id").asText()
        mockMvc.perform(
            post("/api/trainlog/sessions/$firstSessionId/sets").header(HttpHeaders.AUTHORIZATION, bearer(token))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(mapOf("exerciseId" to exerciseId, "setIndex" to 0, "weightKg" to 60.0, "reps" to 10, "rir" to 1))),
        ).andExpect(status().isCreated)
        mockMvc.perform(put("/api/trainlog/sessions/$firstSessionId/finish").header(HttpHeaders.AUTHORIZATION, bearer(token))
            .contentType(MediaType.APPLICATION_JSON).content("{}"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.finishedAt").isNotEmpty())

        val secondSessionId = objectMapper.readTree(
            mockMvc.perform(
                post("/api/trainlog/sessions").header(HttpHeaders.AUTHORIZATION, bearer(token))
                    .contentType(MediaType.APPLICATION_JSON).content("{}"),
            ).andReturn().response.contentAsString,
        ).get("id").asText()

        val prefillResponse = mockMvc.perform(
            get("/api/trainlog/exercises/$exerciseId/prefill").param("repMax", "10").param("targetRir", "2")
                .header(HttpHeaders.AUTHORIZATION, bearer(token)),
        ).andExpect(status().isOk).andReturn().response.contentAsString
        val prefill = objectMapper.readTree(prefillResponse)
        assertEquals(60.0, prefill.get("lastWeightKg").asDouble())
        assertEquals(10, prefill.get("lastReps").asInt())
        // Wiederholungsdach (10) bei niedrigem RIR (1 <= Ziel 2) erreicht -> Gewicht steigt
        assertEquals(62.5, prefill.get("suggestedWeightKg").asDouble())
        // FR-94: Aufwaermsaetze aus dem HEUTE vorgeschlagenen Gewicht (62.5kg, >= 60kg -> 3 Saetze)
        assertEquals(3, prefill.get("warmupSets").size())
        assertEquals(25.0, prefill.get("warmupSets")[0].get("weightKg").asDouble())
        assertEquals(8, prefill.get("warmupSets")[0].get("reps").asInt())
        assertEquals(50.0, prefill.get("warmupSets")[2].get("weightKg").asDouble())

        // clientId-Idempotenz: derselbe Satz zweimal gesendet erzeugt keinen zweiten Eintrag
        val body = objectMapper.writeValueAsString(
            mapOf("exerciseId" to exerciseId, "setIndex" to 0, "weightKg" to 62.5, "reps" to 10, "clientId" to "idem-1"),
        )
        val firstLog = mockMvc.perform(
            post("/api/trainlog/sessions/$secondSessionId/sets").header(HttpHeaders.AUTHORIZATION, bearer(token))
                .contentType(MediaType.APPLICATION_JSON).content(body),
        ).andExpect(status().isCreated).andReturn().response.contentAsString
        val secondLog = mockMvc.perform(
            post("/api/trainlog/sessions/$secondSessionId/sets").header(HttpHeaders.AUTHORIZATION, bearer(token))
                .contentType(MediaType.APPLICATION_JSON).content(body),
        ).andExpect(status().isCreated).andReturn().response.contentAsString
        val firstId = objectMapper.readTree(firstLog).get("set").get("id").asText()
        val secondId = objectMapper.readTree(secondLog).get("set").get("id").asText()
        assertEquals(firstId, secondId)

        val detailResponse = mockMvc.perform(get("/api/trainlog/sessions/$secondSessionId").header(HttpHeaders.AUTHORIZATION, bearer(token)))
            .andExpect(status().isOk).andReturn().response.contentAsString
        assertEquals(1, objectMapper.readTree(detailResponse).get("sets").size())
    }

    @Test
    fun `1rm-verlauf und persoenliche bestwerte spiegeln die geloggten saetze`() {
        val token = registerAndGetToken()
        val exerciseId = anyExerciseId(token)
        val sessionId = objectMapper.readTree(
            mockMvc.perform(
                post("/api/trainlog/sessions").header(HttpHeaders.AUTHORIZATION, bearer(token))
                    .contentType(MediaType.APPLICATION_JSON).content("{}"),
            ).andReturn().response.contentAsString,
        ).get("id").asText()

        for (weight in listOf(80.0, 90.0, 100.0)) {
            mockMvc.perform(
                post("/api/trainlog/sessions/$sessionId/sets").header(HttpHeaders.AUTHORIZATION, bearer(token))
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(mapOf("exerciseId" to exerciseId, "setIndex" to 0, "weightKg" to weight, "reps" to 5))),
            ).andExpect(status().isCreated)
        }

        val historyResponse = mockMvc.perform(get("/api/trainlog/exercises/$exerciseId/one-rep-max-history").header(HttpHeaders.AUTHORIZATION, bearer(token)))
            .andExpect(status().isOk).andReturn().response.contentAsString
        assertEquals(3, objectMapper.readTree(historyResponse).size())

        mockMvc.perform(get("/api/trainlog/exercises/$exerciseId/personal-bests").header(HttpHeaders.AUTHORIZATION, bearer(token)))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.maxWeightKg").value(100.0))
    }

    /** FR-95: alle Teilsaetze einer Dropsatz-Gruppe (auch der Hauptsatz) tragen dieselbe
     * setTechnique + einen ab 0 hochzaehlenden subSetIndex -- siehe V20-Migrationskommentar. */
    @Test
    fun `dropsatz-teilsaetze werden mit fortlaufendem subSetIndex geloggt`() {
        val token = registerAndGetToken()
        val exerciseId = anyExerciseId(token)
        val sessionId = objectMapper.readTree(
            mockMvc.perform(
                post("/api/trainlog/sessions").header(HttpHeaders.AUTHORIZATION, bearer(token))
                    .contentType(MediaType.APPLICATION_JSON).content("{}"),
            ).andReturn().response.contentAsString,
        ).get("id").asText()

        fun logSet(weight: Double, technique: String?, subSetIndex: Int?) = mockMvc.perform(
            post("/api/trainlog/sessions/$sessionId/sets").header(HttpHeaders.AUTHORIZATION, bearer(token))
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    objectMapper.writeValueAsString(
                        mapOf(
                            "exerciseId" to exerciseId, "setIndex" to 0, "weightKg" to weight, "reps" to 8,
                            "setTechnique" to technique, "subSetIndex" to subSetIndex,
                        ),
                    ),
                ),
        )

        val main = logSet(100.0, "dropset", 0).andExpect(status().isCreated).andReturn().response.contentAsString
        val mainSet = objectMapper.readTree(main).get("set")
        assertEquals("dropset", mainSet.get("setTechnique").asText())
        assertEquals(0, mainSet.get("subSetIndex").asInt())

        val drop = logSet(80.0, "dropset", 1).andExpect(status().isCreated).andReturn().response.contentAsString
        val dropSet = objectMapper.readTree(drop).get("set")
        assertEquals("dropset", dropSet.get("setTechnique").asText())
        assertEquals(1, dropSet.get("subSetIndex").asInt())
    }

    @Test
    fun `satztechnik ohne subSetIndex wird abgelehnt`() {
        val token = registerAndGetToken()
        val exerciseId = anyExerciseId(token)
        val sessionId = objectMapper.readTree(
            mockMvc.perform(
                post("/api/trainlog/sessions").header(HttpHeaders.AUTHORIZATION, bearer(token))
                    .contentType(MediaType.APPLICATION_JSON).content("{}"),
            ).andReturn().response.contentAsString,
        ).get("id").asText()

        mockMvc.perform(
            post("/api/trainlog/sessions/$sessionId/sets").header(HttpHeaders.AUTHORIZATION, bearer(token))
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    objectMapper.writeValueAsString(
                        mapOf("exerciseId" to exerciseId, "setIndex" to 0, "weightKg" to 100.0, "reps" to 8, "setTechnique" to "cluster"),
                    ),
                ),
        ).andExpect(status().isUnprocessableEntity)
    }

    @Test
    fun `vorbelegung ohne vorherige saetze schlaegt auch keine aufwaermsaetze vor`() {
        val token = registerAndGetToken()
        val exerciseId = anyExerciseId(token)

        mockMvc.perform(get("/api/trainlog/exercises/$exerciseId/prefill").header(HttpHeaders.AUTHORIZATION, bearer(token)))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.lastWeightKg").doesNotExist())
            .andExpect(jsonPath("$.warmupSets").isEmpty)
    }
}
