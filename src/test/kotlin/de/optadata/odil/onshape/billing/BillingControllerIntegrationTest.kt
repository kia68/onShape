package de.optadata.odil.onshape.billing

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

/** BIZ-01/BIZ-02/BIZ-03. Laeuft mit dem application.properties-Default (kein Stripe-Key gesetzt)
 * -- Checkout/Portal muessen deshalb konsistent 503 "billing_not_configured" liefern, siehe
 * StripeGateway-KDoc. Bewusst OHNE @Transactional, siehe AuthControllerIntegrationTest-Kommentar
 * (Epic Onboarding). */
@SpringBootTest
@AutoConfigureMockMvc
class BillingControllerIntegrationTest : AbstractIntegrationTest() {

    @Autowired lateinit var mockMvc: MockMvc
    @Autowired lateinit var objectMapper: ObjectMapper

    private fun bearer(token: String) = "Bearer $token"

    private fun registerAndGetToken(): String {
        val email = "billing-test-${System.nanoTime()}@example.test"
        val response = mockMvc.perform(
            post("/api/auth/register").contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(RegisterRequest(email, "correct-horse-1", "de"))),
        ).andReturn().response.contentAsString
        return objectMapper.readValue(response, AuthResponse::class.java).token
    }

    @Test
    fun `frisch registrierter nutzer ist free ohne laufzeit`() {
        val token = registerAndGetToken()
        mockMvc.perform(get("/api/billing/subscription").header(HttpHeaders.AUTHORIZATION, bearer(token)))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.tier").value("free"))
            .andExpect(jsonPath("$.isLifetime").value(false))
            .andExpect(jsonPath("$.billingPeriod").doesNotExist())
    }

    @Test
    fun `checkout ohne konfigurierten stripe key liefert 503`() {
        val token = registerAndGetToken()
        mockMvc.perform(
            post("/api/billing/checkout").header(HttpHeaders.AUTHORIZATION, bearer(token))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(mapOf("plan" to "plus_monthly"))),
        ).andExpect(status().isServiceUnavailable).andExpect(jsonPath("$.code").value("billing_not_configured"))
    }

    @Test
    fun `checkout mit unbekanntem plan wird abgelehnt`() {
        val token = registerAndGetToken()
        mockMvc.perform(
            post("/api/billing/checkout").header(HttpHeaders.AUTHORIZATION, bearer(token))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(mapOf("plan" to "does_not_exist"))),
        ).andExpect(status().isBadRequest)
    }

    @Test
    fun `billing-portal ohne bisherigen kauf liefert 422`() {
        val token = registerAndGetToken()
        mockMvc.perform(post("/api/billing/portal").header(HttpHeaders.AUTHORIZATION, bearer(token)))
            .andExpect(status().isUnprocessableEntity).andExpect(jsonPath("$.code").value("no_stripe_customer"))
    }

    @Test
    fun `webhook ohne konfiguriertes secret liefert 503`() {
        mockMvc.perform(
            post("/api/billing/webhook")
                .header("Stripe-Signature", "t=1,v1=irrelevant")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{}"),
        ).andExpect(status().isServiceUnavailable)
    }
}
