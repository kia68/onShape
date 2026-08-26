package de.optadata.odil.onshape.training

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
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import tools.jackson.databind.ObjectMapper
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue

/** FR-70..FR-75, FR-77. Bewusst OHNE @Transactional, siehe AuthControllerIntegrationTest-Kommentar
 * (Epic Onboarding): RLS-Session-Variablen brauchen echte, separate, committete Transaktionen
 * pro HTTP-Request. */
@SpringBootTest
@AutoConfigureMockMvc
class ProgramControllerIntegrationTest : AbstractIntegrationTest() {

    @Autowired lateinit var mockMvc: MockMvc
    @Autowired lateinit var objectMapper: ObjectMapper

    private fun bearer(token: String) = "Bearer $token"

    private fun registerOnboardedUser(trainingDaysWeek: Int = 3, experience: String = "beginner"): String {
        val email = "training-test-${System.nanoTime()}@example.test"
        val registerResponse = mockMvc.perform(
            post("/api/auth/register").contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(RegisterRequest(email, "correct-horse-1", "de"))),
        ).andReturn().response.contentAsString
        val token = objectMapper.readValue(registerResponse, AuthResponse::class.java).token

        mockMvc.perform(
            org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put("/api/onboarding/profile")
                .header(HttpHeaders.AUTHORIZATION, bearer(token))
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    objectMapper.writeValueAsString(
                        mapOf(
                            "sex" to "male",
                            "birthDate" to "1995-05-01",
                            "heightCm" to 180.0,
                            "weightKg" to 80.0,
                            "experience" to experience,
                            "activityPal" to 1.4,
                            "goal" to "maintain",
                            "goalRatePctWeek" to 0.0,
                            "dietaryPrefs" to emptyList<String>(),
                            "allergens" to emptyList<String>(),
                            "injuries" to emptyList<String>(),
                            "equipment" to listOf("bodyweight", "dumbbells", "gym", "barbell", "bands", "pullup_bar", "kettlebell"),
                            "trainingDaysWeek" to trainingDaysWeek,
                            "sessionMinutes" to 60,
                            "healthScreening" to mapOf(
                                "heartCondition" to false, "pregnancy" to false, "recentInjury" to false, "medication" to false,
                            ),
                        ),
                    ),
                ),
        ).andExpect(status().isOk)
        return token
    }

    @Test
    fun `plan generieren erzeugt fuer jede woche und jeden tag einen eintrag`() {
        val token = registerOnboardedUser(trainingDaysWeek = 3)

        val response = mockMvc.perform(
            post("/api/training/programs/generate").header(HttpHeaders.AUTHORIZATION, bearer(token))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(mapOf("weeks" to 6))),
        ).andExpect(status().isCreated)
            .andExpect(jsonPath("$.weeks").value(6))
            .andExpect(jsonPath("$.isActive").value(true))
            .andReturn().response.contentAsString

        val json = objectMapper.readTree(response)
        assertEquals(6 * json.get("daysPerWeek").asInt(), json.get("days").size())
        assertTrue(json.get("days").get(0).get("items").size() > 0)
    }

    @Test
    fun `aktiven plan abrufen liefert den zuletzt generierten plan`() {
        val token = registerOnboardedUser()
        mockMvc.perform(
            post("/api/training/programs/generate").header(HttpHeaders.AUTHORIZATION, bearer(token))
                .contentType(MediaType.APPLICATION_JSON).content(objectMapper.writeValueAsString(mapOf("weeks" to 4))),
        ).andExpect(status().isCreated)

        mockMvc.perform(get("/api/training/programs/active").header(HttpHeaders.AUTHORIZATION, bearer(token)))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.weeks").value(4))
            .andExpect(jsonPath("$.isActive").value(true))
    }

    @Test
    fun `split-override erzwingt den angeforderten split`() {
        val token = registerOnboardedUser(trainingDaysWeek = 3, experience = "advanced")

        mockMvc.perform(
            post("/api/training/programs/generate").header(HttpHeaders.AUTHORIZATION, bearer(token))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(mapOf("weeks" to 2, "splitTypeOverride" to "ppl"))),
        ).andExpect(status().isCreated)
            .andExpect(jsonPath("$.splitType").value("ppl"))
            .andExpect(jsonPath("$.daysPerWeek").value(6))
    }

    @Test
    fun `uebung tauschen ersetzt sie in allen wochen und die alte uebung verschwindet`() {
        val token = registerOnboardedUser(trainingDaysWeek = 3, experience = "advanced")
        val generateResponse = mockMvc.perform(
            post("/api/training/programs/generate").header(HttpHeaders.AUTHORIZATION, bearer(token))
                .contentType(MediaType.APPLICATION_JSON).content(objectMapper.writeValueAsString(mapOf("weeks" to 3))),
        ).andReturn().response.contentAsString
        val program = objectMapper.readTree(generateResponse)
        val programId = program.get("id").asText()
        val oldExerciseId = program.get("days").get(0).get("items").get(0).get("exerciseId").asText()
        val occurrencesBefore = countOccurrences(program, oldExerciseId)
        assertTrue(occurrencesBefore > 0)

        val swapResponse = mockMvc.perform(
            post("/api/training/programs/$programId/items/$oldExerciseId/swap").header(HttpHeaders.AUTHORIZATION, bearer(token))
                .contentType(MediaType.APPLICATION_JSON).content(objectMapper.writeValueAsString(mapOf("reason" to "dislike"))),
        ).andExpect(status().isOk).andReturn().response.contentAsString
        val swap = objectMapper.readTree(swapResponse)
        val newExerciseId = swap.get("replacementExerciseId").asText()
        assertNotEquals(oldExerciseId, newExerciseId)
        // Alternative koennte (in einem anderen Tagesindex) bereits im Plan vorkommen -- die neue
        // Gesamtzahl ist deshalb die alte Zahl PLUS was schon vorher an anderer Stelle da war.
        val newExerciseBaseline = countOccurrences(program, newExerciseId)

        val updatedProgram = swap.get("program")
        assertEquals(0, countOccurrences(updatedProgram, oldExerciseId))
        assertEquals(occurrencesBefore + newExerciseBaseline, countOccurrences(updatedProgram, newExerciseId))
    }

    @Test
    fun `volumen-dashboard liefert geplante saetze pro muskel innerhalb des korridors`() {
        val token = registerOnboardedUser(trainingDaysWeek = 4, experience = "intermediate")
        mockMvc.perform(
            post("/api/training/programs/generate").header(HttpHeaders.AUTHORIZATION, bearer(token))
                .contentType(MediaType.APPLICATION_JSON).content(objectMapper.writeValueAsString(mapOf("weeks" to 6))),
        ).andExpect(status().isCreated)

        val response = mockMvc.perform(
            get("/api/training/programs/active/volume").param("week", "1").header(HttpHeaders.AUTHORIZATION, bearer(token)),
        ).andExpect(status().isOk).andReturn().response.contentAsString
        val dashboard = objectMapper.readTree(response)
        assertEquals(1, dashboard.get("weekNumber").asInt())
        assertTrue(dashboard.get("entries").size() > 0)
        val first = dashboard.get("entries")[0]
        assertTrue(first.get("plannedSets").asDouble() > 0.0)
        assertTrue(first.get("corridorMax").asInt() > 0)
    }

    @Test
    fun `manueller plan wird angelegt und kann als aktiv gesetzt werden`() {
        val token = registerOnboardedUser()
        val exercisesResponse = mockMvc.perform(get("/api/training/exercises").header(HttpHeaders.AUTHORIZATION, bearer(token)))
            .andExpect(status().isOk).andReturn().response.contentAsString
        val squat = objectMapper.readTree(exercisesResponse).first { it.get("pattern").asText() == "squat" && it.get("equipment").toString().contains("bodyweight") }
        val squatId = squat.get("id").asText()

        val createResponse = mockMvc.perform(
            post("/api/training/programs").header(HttpHeaders.AUTHORIZATION, bearer(token))
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    objectMapper.writeValueAsString(
                        mapOf(
                            "name" to "Mein eigener Plan",
                            "goal" to "maintain",
                            "daysPerWeek" to 1,
                            "weeks" to 1,
                            "splitType" to "full_body",
                            "days" to listOf(
                                mapOf(
                                    "weekNumber" to 1, "dayIndex" to 0, "name" to "Tag A", "isDeload" to false,
                                    "items" to listOf(
                                        mapOf(
                                            "exerciseId" to squatId, "sortOrder" to 0, "sets" to 3,
                                            "repMin" to 8, "repMax" to 12, "durationMinutes" to null, "targetRir" to 2, "restSeconds" to 90,
                                        ),
                                    ),
                                ),
                            ),
                        ),
                    ),
                ),
        ).andExpect(status().isCreated).andReturn().response.contentAsString
        val programId = objectMapper.readTree(createResponse).get("id").asText()

        mockMvc.perform(
            org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put("/api/training/programs/$programId/active")
                .header(HttpHeaders.AUTHORIZATION, bearer(token)),
        ).andExpect(status().isOk)
            .andExpect(jsonPath("$.generatedBy").value("manual"))
            .andExpect(jsonPath("$.isActive").value(true))
    }

    @Test
    fun `manueller plan mit reps und dauer gleichzeitig wird abgelehnt`() {
        val token = registerOnboardedUser()
        val exercisesResponse = mockMvc.perform(get("/api/training/exercises").header(HttpHeaders.AUTHORIZATION, bearer(token)))
            .andExpect(status().isOk).andReturn().response.contentAsString
        val anyExerciseId = objectMapper.readTree(exercisesResponse).get(0).get("id").asText()

        mockMvc.perform(
            post("/api/training/programs").header(HttpHeaders.AUTHORIZATION, bearer(token))
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    objectMapper.writeValueAsString(
                        mapOf(
                            "name" to "Ungueltiger Plan", "goal" to "maintain", "daysPerWeek" to 1, "weeks" to 1, "splitType" to "full_body",
                            "days" to listOf(
                                mapOf(
                                    "weekNumber" to 1, "dayIndex" to 0, "name" to "Tag A", "isDeload" to false,
                                    "items" to listOf(
                                        mapOf(
                                            "exerciseId" to anyExerciseId, "sortOrder" to 0, "sets" to 3,
                                            "repMin" to 8, "repMax" to 12, "durationMinutes" to 10, "targetRir" to 2, "restSeconds" to 90,
                                        ),
                                    ),
                                ),
                            ),
                        ),
                    ),
                ),
        ).andExpect(status().isUnprocessableEntity)
    }

    /** BIZ-01 (§15.1 "Trainingsplan-Generator: 1 aktiver Plan" im Free-Tier). Frisch registrierte
     * Nutzer ohne Abo sind FREE (siehe SubscriptionService). */
    @Test
    fun `free-tier darf nur ein programm erstellen, zweite generierung wird geblockt`() {
        val token = registerOnboardedUser()
        mockMvc.perform(
            post("/api/training/programs/generate").header(HttpHeaders.AUTHORIZATION, bearer(token))
                .contentType(MediaType.APPLICATION_JSON).content(objectMapper.writeValueAsString(mapOf("weeks" to 4))),
        ).andExpect(status().isCreated)

        mockMvc.perform(
            post("/api/training/programs/generate").header(HttpHeaders.AUTHORIZATION, bearer(token))
                .contentType(MediaType.APPLICATION_JSON).content(objectMapper.writeValueAsString(mapOf("weeks" to 4))),
        ).andExpect(status().isUnprocessableEntity).andExpect(jsonPath("$.code").value("program_limit_exceeded"))
    }

    private fun countOccurrences(program: tools.jackson.databind.JsonNode, exerciseId: String): Int =
        program.get("days").sumOf { day -> day.get("items").count { it.get("exerciseId").asText() == exerciseId } }
}
