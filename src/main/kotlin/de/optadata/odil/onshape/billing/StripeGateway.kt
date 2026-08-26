package de.optadata.odil.onshape.billing

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import org.springframework.web.client.RestClient
import java.net.URLEncoder
import java.nio.charset.StandardCharsets
import java.util.Base64

data class StripeCheckoutSession(val id: String, val url: String)

/**
 * BIZ-02 (KONZEPT.md §15, §11.3 "Stripe mit EU-Entitaet"): handgerollter REST-Client statt dem
 * `stripe-java`-SDK, gleiches Muster wie [de.optadata.odil.onshape.foodimport.UsdaFoodDataCentralClient]/
 * OpenFoodFactsClient -- Stripes API ist reines Formular-POST + JSON-Antwort, eine SDK-
 * Abhaengigkeit fuer zwei Endpunkte lohnt sich nicht.
 *
 * Aktiv nur, wenn `stripe.secret-key` echt gesetzt ist -- ohne echten Key wirft jeder Aufruf
 * [BillingNotConfiguredException] (503), bevor irgendein Netzwerkzugriff versucht wird. Gleiches
 * Muster wie OAuth2 (SecurityConfig) und USDA/BLS (foodimport): die App startet und laeuft auch
 * ohne echten Stripe-Account, nur die Checkout-/Portal-Endpunkte bleiben inaktiv. In dieser
 * Session gibt es keinen echten (auch keinen Test-Mode-) Stripe-Account -- ein echter Checkout-
 * Flow konnte deshalb NICHT live im Browser verifiziert werden, siehe docs/progress.md.
 */
@Component
class StripeGateway(
    @Value("\${stripe.secret-key:}") private val secretKey: String,
    @Value("\${stripe.api-base-url:https://api.stripe.com}") baseUrl: String,
) {
    val isConfigured: Boolean get() = secretKey.isNotBlank()

    private val restClient = RestClient.builder().baseUrl(baseUrl).build()

    fun createCheckoutSession(fields: Map<String, String>): StripeCheckoutSession {
        requireConfigured()
        val response = restClient.post()
            .uri("/v1/checkout/sessions")
            .header("Authorization", basicAuthHeader())
            .header("Content-Type", "application/x-www-form-urlencoded")
            .body(fields.toFormBody())
            .retrieve()
            .body(StripeCheckoutSessionApiResponse::class.java)
            ?: error("Stripe checkout/sessions: leere Antwort")
        return StripeCheckoutSession(response.id, response.url)
    }

    fun createBillingPortalSession(customerId: String, returnUrl: String): String {
        requireConfigured()
        val response = restClient.post()
            .uri("/v1/billing_portal/sessions")
            .header("Authorization", basicAuthHeader())
            .header("Content-Type", "application/x-www-form-urlencoded")
            .body(mapOf("customer" to customerId, "return_url" to returnUrl).toFormBody())
            .retrieve()
            .body(StripeBillingPortalApiResponse::class.java)
            ?: error("Stripe billing_portal/sessions: leere Antwort")
        return response.url
    }

    private fun requireConfigured() {
        if (!isConfigured) throw BillingNotConfiguredException()
    }

    /** Stripe nutzt HTTP Basic Auth mit dem Secret Key als Username, leeres Passwort. */
    private fun basicAuthHeader(): String =
        "Basic " + Base64.getEncoder().encodeToString("$secretKey:".toByteArray(StandardCharsets.UTF_8))

    private fun Map<String, String>.toFormBody(): String =
        entries.joinToString("&") { (k, v) -> "${urlEncode(k)}=${urlEncode(v)}" }

    private fun urlEncode(v: String) = URLEncoder.encode(v, StandardCharsets.UTF_8)
}

@JsonIgnoreProperties(ignoreUnknown = true)
private data class StripeCheckoutSessionApiResponse(val id: String, val url: String)

@JsonIgnoreProperties(ignoreUnknown = true)
private data class StripeBillingPortalApiResponse(val url: String)
