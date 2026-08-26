package de.optadata.odil.onshape.wellbeing

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
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import tools.jackson.databind.ObjectMapper
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/** LEGAL-12 (KONZEPT.md §14.5). Bewusst OHNE @Transactional, siehe
 * AuthControllerIntegrationTest-Kommentar (Epic Onboarding). */
@SpringBootTest
@AutoConfigureMockMvc
class WellbeingControllerIntegrationTest : AbstractIntegrationTest() {

    @Autowired lateinit var mockMvc: MockMvc
    @Autowired lateinit var objectMapper: ObjectMapper

    private fun bearer(token: String) = "Bearer $token"

    private fun register(): String {
        val email = "wellbeing-test-${System.nanoTime()}@example.test"
        val response = mockMvc.perform(
            post("/api/auth/register").contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(RegisterRequest(email, "correct-horse-1", "de"))),
        ).andReturn().response.contentAsString
        return objectMapper.readValue(response, AuthResponse::class.java).token
    }

    private fun registerAndOnboard(): String {
        val token = register()
        mockMvc.perform(
            put("/api/onboarding/profile").header(HttpHeaders.AUTHORIZATION, bearer(token))
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    objectMapper.writeValueAsString(
                        mapOf(
                            "sex" to "male", "birthDate" to "1995-05-01", "heightCm" to 180.0, "weightKg" to 80.0,
                            "experience" to "intermediate", "activityPal" to 1.4, "goal" to "maintain", "goalRatePctWeek" to 0.0,
                            "dietaryPrefs" to emptyList<String>(), "allergens" to emptyList<String>(), "injuries" to emptyList<String>(),
                            "equipment" to listOf("bodyweight"), "trainingDaysWeek" to 3, "sessionMinutes" to 45,
                            "healthScreening" to mapOf("heartCondition" to false, "pregnancy" to false, "recentInjury" to false, "medication" to false),
                        ),
                    ),
                ),
        ).andExpect(status().isOk)
        return token
    }

    @Test
    fun `frisch onboardeter nutzer hat keine wellbeing-flags`() {
        val token = registerAndOnboard()
        val response = mockMvc.perform(get("/api/wellbeing/guardrail-status").header(HttpHeaders.AUTHORIZATION, bearer(token)))
            .andExpect(status().isOk).andReturn().response.contentAsString
        val json = objectMapper.readTree(response)
        assertFalse(json.get("hideCalorieDisplay").asBoolean())
        assertEquals(0, json.get("flags").size())
    }

    @Test
    fun `training an sieben tagen in folge blendet die kalorienanzeige aus`() {
        val token = registerAndOnboard()
        repeat(7) {
            mockMvc.perform(
                post("/api/trainlog/sessions").header(HttpHeaders.AUTHORIZATION, bearer(token))
                    .contentType(MediaType.APPLICATION_JSON).content("{}"),
            ).andExpect(status().isCreated)
        }

        val response = mockMvc.perform(get("/api/wellbeing/guardrail-status").header(HttpHeaders.AUTHORIZATION, bearer(token)))
            .andExpect(status().isOk).andReturn().response.contentAsString
        val json = objectMapper.readTree(response)
        assertTrue(json.get("hideCalorieDisplay").asBoolean())
        assertTrue(json.get("flags").toString().contains("EXCESSIVE_TRAINING"))
        assertTrue(json.get("resources").size() > 0)
    }

    @Test
    fun `pausenmodus laesst sich aktivieren und wieder aufheben`() {
        val token = registerAndOnboard()

        val before = objectMapper.readTree(
            mockMvc.perform(get("/api/wellbeing/pause-status").header(HttpHeaders.AUTHORIZATION, bearer(token)))
                .andExpect(status().isOk).andReturn().response.contentAsString,
        )
        assertFalse(before.get("trackingPaused").asBoolean())

        val paused = objectMapper.readTree(
            mockMvc.perform(post("/api/wellbeing/pause").header(HttpHeaders.AUTHORIZATION, bearer(token)))
                .andExpect(status().isOk).andReturn().response.contentAsString,
        )
        assertTrue(paused.get("trackingPaused").asBoolean())
        assertFalse(paused.get("trackingPausedAt").isNull)

        val resumed = objectMapper.readTree(
            mockMvc.perform(post("/api/wellbeing/resume").header(HttpHeaders.AUTHORIZATION, bearer(token)))
                .andExpect(status().isOk).andReturn().response.contentAsString,
        )
        assertFalse(resumed.get("trackingPaused").asBoolean())
    }

    @Test
    fun `pausenmodus vor dem onboarding liefert 404`() {
        val token = register()
        mockMvc.perform(post("/api/wellbeing/pause").header(HttpHeaders.AUTHORIZATION, bearer(token)))
            .andExpect(status().isNotFound)
    }
}
