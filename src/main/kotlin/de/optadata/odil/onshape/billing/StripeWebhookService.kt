package de.optadata.odil.onshape.billing

import com.fasterxml.jackson.annotation.JsonAlias
import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import tools.jackson.databind.ObjectMapper
import java.util.UUID

/**
 * BIZ-02: verarbeitet zwei Event-Typen -- `checkout.session.completed` (Kauf abgeschlossen,
 * FR-Aequivalent fuer Abo UND Lifetime-Deal) und `customer.subscription.deleted` (Kuendigung
 * bzw. endgueltiger Zahlungsausfall nach Stripes eigenem Dunning). Alle anderen Event-Typen
 * (`customer.subscription.updated` fuer Plan-Wechsel/`cancel_at_period_end`,
 * `invoice.payment_failed` fuer fruehe Warnung vor dem endgueltigen Ausfall) werden bewusst
 * NICHT verarbeitet -- kein Tier-Gate haengt fachlich daran (siehe SubscriptionService-KDoc),
 * eine vollstaendige Abbildung aller Stripe-Lifecycle-Events waere Vorratsarbeit ohne aktuellen
 * Bedarf. Unbekannte Event-Typen werden mit 200 quittiert (Stripe wiederholt sonst unbegrenzt).
 */
@Service
class StripeWebhookService(
    private val subscriptionService: SubscriptionService,
    private val objectMapper: ObjectMapper,
    @Value("\${stripe.webhook-secret:}") private val webhookSecret: String,
) {

    fun handle(payload: String, sigHeader: String?) {
        if (webhookSecret.isBlank()) throw BillingNotConfiguredException()
        if (sigHeader == null || !StripeSignatureVerifier.verify(payload, sigHeader, webhookSecret)) {
            throw InvalidWebhookSignatureException()
        }

        val event = objectMapper.readValue(payload, StripeEvent::class.java)
        when (event.type) {
            "checkout.session.completed" -> handleCheckoutCompleted(event.data.`object`)
            "customer.subscription.deleted" -> handleSubscriptionDeleted(event.data.`object`)
            else -> Unit
        }
    }

    private fun handleCheckoutCompleted(session: StripeEventObject) {
        val userId = session.clientReferenceId?.let { UUID.fromString(it) } ?: return
        val planKey = session.metadata?.get("plan") ?: return
        val plan = CheckoutPlan.entries.firstOrNull { it.dbValue == planKey } ?: return
        val customerId = session.customer ?: return

        // current_period_end bleibt bewusst leer: checkout.session.completed liefert es nicht
        // im Payload selbst (nur die Subscription-/Customer-IDs), ein zusaetzlicher Retrieve-
        // Call bzw. das Auswerten von customer.subscription.updated ist hier nicht umgesetzt --
        // kein Gate haengt fachlich an diesem Feld, es ist reine Anzeige-Information.
        subscriptionService.grantFromCheckout(
            userId = userId,
            plan = plan,
            stripeCustomerId = customerId,
            stripeSubscriptionId = session.subscription,
            currentPeriodEnd = null,
        )
    }

    private fun handleSubscriptionDeleted(subscription: StripeEventObject) {
        val subscriptionId = subscription.id ?: return
        subscriptionService.revokeByStripeSubscriptionId(subscriptionId)
    }
}

@JsonIgnoreProperties(ignoreUnknown = true)
data class StripeEvent(val id: String? = null, val type: String? = null, val data: StripeEventData = StripeEventData())

@JsonIgnoreProperties(ignoreUnknown = true)
data class StripeEventData(val `object`: StripeEventObject = StripeEventObject())

@JsonIgnoreProperties(ignoreUnknown = true)
data class StripeEventObject(
    val id: String? = null,
    val customer: String? = null,
    val subscription: String? = null,
    @JsonAlias("client_reference_id") val clientReferenceId: String? = null,
    val metadata: Map<String, String>? = null,
)
