package de.optadata.odil.onshape.billing

import de.optadata.odil.onshape.AbstractIntegrationTest
import de.optadata.odil.onshape.auth.AuthResponse
import de.optadata.odil.onshape.auth.RegisterRequest
import java.time.Instant
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc
import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType
import org.springframework.test.context.TestPropertySource
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import tools.jackson.databind.ObjectMapper

/** BIZ-02: eigener, isolierter Kontext mit echtem Webhook-Secret (siehe
 * AuthRateLimitFilterIntegrationTest-Kommentar fuer das @TestPropertySource-Pattern) -- so laesst
 * sich der volle Checkout->Webhook->Tier-Grant-Kreislauf ohne echten Stripe-Account end-to-end
 * pruefen: die Signatur wird hier genauso berechnet, wie Stripe es in Produktion taete. */
@SpringBootTest
@AutoConfigureMockMvc
@TestPropertySource(properties = ["stripe.webhook-secret=whsec_integration_test"])
class BillingWebhookIntegrationTest : AbstractIntegrationTest() {

    @Autowired lateinit var mockMvc: MockMvc
    @Autowired lateinit var objectMapper: ObjectMapper

    private val webhookSecret = "whsec_integration_test"

    private fun bearer(token: String) = "Bearer $token"

    private fun registerAndGetToken(): Pair<String, String> {
        val email = "billing-webhook-test-${System.nanoTime()}@example.test"
        val response = mockMvc.perform(
            post("/api/auth/register").contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(RegisterRequest(email, "correct-horse-1", "de"))),
        ).andReturn().response.contentAsString
        val auth = objectMapper.readValue(response, AuthResponse::class.java)
        return auth.token to auth.userId.toString()
    }

    private fun signatureHeader(payload: String, timestamp: Long = Instant.now().epochSecond): String {
        val mac = Mac.getInstance("HmacSHA256")
        mac.init(SecretKeySpec(webhookSecret.toByteArray(), "HmacSHA256"))
        val hex = mac.doFinal("$timestamp.$payload".toByteArray()).joinToString("") { "%02x".format(it) }
        return "t=$timestamp,v1=$hex"
    }

    @Test
    fun `checkout session completed webhook schaltet den plan frei`() {
        val (token, userId) = registerAndGetToken()
        val payload = """
            {"id":"evt_1","type":"checkout.session.completed","data":{"object":{
              "client_reference_id":"$userId","customer":"cus_test_1","subscription":"sub_test_1",
              "metadata":{"plan":"plus_yearly"}
            }}}
        """.trimIndent()

        mockMvc.perform(
            post("/api/billing/webhook").header("Stripe-Signature", signatureHeader(payload))
                .contentType(MediaType.APPLICATION_JSON).content(payload),
        ).andExpect(status().isOk)

        mockMvc.perform(get("/api/billing/subscription").header(HttpHeaders.AUTHORIZATION, bearer(token)))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.tier").value("plus"))
            .andExpect(jsonPath("$.billingPeriod").value("yearly"))
            .andExpect(jsonPath("$.isLifetime").value(false))
    }

    @Test
    fun `lifetime checkout schaltet dauerhaft coach frei`() {
        val (token, userId) = registerAndGetToken()
        val payload = """
            {"id":"evt_2","type":"checkout.session.completed","data":{"object":{
              "client_reference_id":"$userId","customer":"cus_test_2",
              "metadata":{"plan":"lifetime"}
            }}}
        """.trimIndent()

        mockMvc.perform(
            post("/api/billing/webhook").header("Stripe-Signature", signatureHeader(payload))
                .contentType(MediaType.APPLICATION_JSON).content(payload),
        ).andExpect(status().isOk)

        mockMvc.perform(get("/api/billing/subscription").header(HttpHeaders.AUTHORIZATION, bearer(token)))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.tier").value("coach"))
            .andExpect(jsonPath("$.billingPeriod").value("lifetime"))
            .andExpect(jsonPath("$.isLifetime").value(true))
    }

    @Test
    fun `subscription deleted webhook faellt zurueck auf free`() {
        val (token, userId) = registerAndGetToken()
        val grantPayload = """
            {"id":"evt_3","type":"checkout.session.completed","data":{"object":{
              "client_reference_id":"$userId","customer":"cus_test_3","subscription":"sub_test_3",
              "metadata":{"plan":"coach_monthly"}
            }}}
        """.trimIndent()
        mockMvc.perform(
            post("/api/billing/webhook").header("Stripe-Signature", signatureHeader(grantPayload))
                .contentType(MediaType.APPLICATION_JSON).content(grantPayload),
        ).andExpect(status().isOk)

        val cancelPayload = """{"id":"evt_4","type":"customer.subscription.deleted","data":{"object":{"id":"sub_test_3"}}}"""
        mockMvc.perform(
            post("/api/billing/webhook").header("Stripe-Signature", signatureHeader(cancelPayload))
                .contentType(MediaType.APPLICATION_JSON).content(cancelPayload),
        ).andExpect(status().isOk)

        mockMvc.perform(get("/api/billing/subscription").header(HttpHeaders.AUTHORIZATION, bearer(token)))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.tier").value("free"))
    }

    @Test
    fun `webhook mit falscher signatur wird abgelehnt`() {
        val payload = """{"id":"evt_5","type":"checkout.session.completed","data":{"object":{}}}"""
        mockMvc.perform(
            post("/api/billing/webhook").header("Stripe-Signature", "t=1,v1=deadbeef")
                .contentType(MediaType.APPLICATION_JSON).content(payload),
        ).andExpect(status().isBadRequest).andExpect(jsonPath("$.code").value("invalid_webhook_signature"))
    }
}
