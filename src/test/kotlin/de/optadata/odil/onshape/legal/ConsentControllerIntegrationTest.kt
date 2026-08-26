package de.optadata.odil.onshape.legal

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
import kotlin.test.assertFalse

/** LEGAL-11 (KONZEPT.md §14.1): granulare, jederzeit widerrufbare Einwilligungen. Bewusst OHNE
 * @Transactional, siehe AuthControllerIntegrationTest-Kommentar (Epic Onboarding). */
@SpringBootTest
@AutoConfigureMockMvc
class ConsentControllerIntegrationTest : AbstractIntegrationTest() {

    @Autowired lateinit var mockMvc: MockMvc
    @Autowired lateinit var objectMapper: ObjectMapper

    private fun bearer(token: String) = "Bearer $token"

    private fun register(): String {
        val email = "consent-test-${System.nanoTime()}@example.test"
        val response = mockMvc.perform(
            post("/api/auth/register").contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(RegisterRequest(email, "correct-horse-1", "de"))),
        ).andReturn().response.contentAsString
        return objectMapper.readValue(response, AuthResponse::class.java).token
    }

    @Test
    fun `frisch registrierter nutzer hat noch keine erteilten einwilligungen`() {
        val token = register()
        val response = mockMvc.perform(get("/api/consents").header(HttpHeaders.AUTHORIZATION, bearer(token)))
            .andExpect(status().isOk).andReturn().response.contentAsString
        val json = objectMapper.readTree(response)
        assertEquals(5, json.size())
        assertTrueAllUngranted(json)
    }

    private fun assertTrueAllUngranted(json: tools.jackson.databind.JsonNode) {
        json.forEach { assertFalse(it.get("granted").asBoolean()) }
    }

    @Test
    fun `einwilligungsschritt ohne core wird abgelehnt`() {
        val token = register()
        mockMvc.perform(
            put("/api/consents").header(HttpHeaders.AUTHORIZATION, bearer(token))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(mapOf("core" to false, "photoAi" to false, "wearableSync" to false, "analytics" to false, "marketing" to false))),
        ).andExpect(status().isUnprocessableEntity).andExpect(jsonPath("$.code").value("core_consent_required"))
    }

    @Test
    fun `ablehnung einzelner zwecke blockiert die anderen nicht`() {
        val token = register()
        val response = mockMvc.perform(
            put("/api/consents").header(HttpHeaders.AUTHORIZATION, bearer(token))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(mapOf("core" to true, "photoAi" to true, "wearableSync" to false, "analytics" to false, "marketing" to false))),
        ).andExpect(status().isOk).andReturn().response.contentAsString
        val json = objectMapper.readTree(response)
        val byPurpose = json.associateBy({ it.get("purpose").asText() }, { it.get("granted").asBoolean() })
        assertEquals(true, byPurpose["core"])
        assertEquals(true, byPurpose["photo_ai"])
        assertEquals(false, byPurpose["wearable_sync"])
        assertEquals(false, byPurpose["analytics"])
        assertEquals(false, byPurpose["marketing"])
    }

    @Test
    fun `einzelner zweck laesst sich spaeter jederzeit widerrufen und erneut erteilen`() {
        val token = register()
        mockMvc.perform(
            put("/api/consents").header(HttpHeaders.AUTHORIZATION, bearer(token))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(mapOf("core" to true, "photoAi" to true, "wearableSync" to false, "analytics" to false, "marketing" to false))),
        ).andExpect(status().isOk)

        mockMvc.perform(
            put("/api/consents/photo_ai").header(HttpHeaders.AUTHORIZATION, bearer(token))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(mapOf("granted" to false))),
        ).andExpect(status().isOk).andExpect(jsonPath("$[?(@.purpose == 'photo_ai')].granted").value(false))

        mockMvc.perform(
            put("/api/consents/marketing").header(HttpHeaders.AUTHORIZATION, bearer(token))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(mapOf("granted" to true))),
        ).andExpect(status().isOk).andExpect(jsonPath("$[?(@.purpose == 'marketing')].granted").value(true))
    }

    @Test
    fun `core einwilligung kann nicht widerrufen werden`() {
        val token = register()
        mockMvc.perform(
            put("/api/consents").header(HttpHeaders.AUTHORIZATION, bearer(token))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(mapOf("core" to true, "photoAi" to false, "wearableSync" to false, "analytics" to false, "marketing" to false))),
        ).andExpect(status().isOk)

        mockMvc.perform(
            put("/api/consents/core").header(HttpHeaders.AUTHORIZATION, bearer(token))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(mapOf("granted" to false))),
        ).andExpect(status().isUnprocessableEntity).andExpect(jsonPath("$.code").value("core_consent_immutable"))
    }
}
