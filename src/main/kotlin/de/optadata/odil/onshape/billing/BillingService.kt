package de.optadata.odil.onshape.billing

import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import java.util.UUID

/** BIZ-02/BIZ-03: baut die Stripe-Checkout-Felder aus einem [CheckoutPlan] zusammen und haengt
 * den Lifetime-Deckel (§15.1) davor. `client_reference_id`/`metadata[plan]` sind der Weg, wie
 * der Webhook (kein eingeloggter Request) spaeter wieder weiss, welcher Nutzer welchen Plan
 * gekauft hat (siehe StripeWebhookService). */
@Service
class BillingService(
    private val stripeGateway: StripeGateway,
    private val subscriptionService: SubscriptionService,
    @Value("\${stripe.price-plus-monthly:}") private val pricePlusMonthly: String,
    @Value("\${stripe.price-plus-yearly:}") private val pricePlusYearly: String,
    @Value("\${stripe.price-coach-monthly:}") private val priceCoachMonthly: String,
    @Value("\${stripe.price-coach-yearly:}") private val priceCoachYearly: String,
    @Value("\${stripe.price-lifetime:}") private val priceLifetime: String,
    @Value("\${stripe.checkout-success-url:http://localhost:3000/de/settings/billing?checkout=success}") private val successUrl: String,
    @Value("\${stripe.checkout-cancel-url:http://localhost:3000/de/settings/billing?checkout=cancel}") private val cancelUrl: String,
    @Value("\${stripe.billing-portal-return-url:http://localhost:3000/de/settings/billing}") private val portalReturnUrl: String,
    @Value("\${billing.lifetime-deal-cap:5000}") private val lifetimeDealCap: Int,
) {

    fun startCheckout(userId: UUID, plan: CheckoutPlan): StripeCheckoutSession {
        if (plan == CheckoutPlan.LIFETIME && subscriptionService.lifetimeCapReached(lifetimeDealCap)) {
            throw LifetimeCapReachedException(lifetimeDealCap)
        }
        val priceId = priceIdFor(plan)
        if (priceId.isBlank()) throw BillingNotConfiguredException()

        val mode = if (plan.period == BillingPeriod.LIFETIME) "payment" else "subscription"
        return stripeGateway.createCheckoutSession(
            mapOf(
                "mode" to mode,
                "line_items[0][price]" to priceId,
                "line_items[0][quantity]" to "1",
                "success_url" to successUrl,
                "cancel_url" to cancelUrl,
                "client_reference_id" to userId.toString(),
                "metadata[plan]" to plan.dbValue,
            ),
        )
    }

    fun startBillingPortal(userId: UUID): String {
        val customerId = subscriptionService.current(userId)?.stripeCustomerId ?: throw NoStripeCustomerException()
        return stripeGateway.createBillingPortalSession(customerId, portalReturnUrl)
    }

    private fun priceIdFor(plan: CheckoutPlan): String = when (plan) {
        CheckoutPlan.PLUS_MONTHLY -> pricePlusMonthly
        CheckoutPlan.PLUS_YEARLY -> pricePlusYearly
        CheckoutPlan.COACH_MONTHLY -> priceCoachMonthly
        CheckoutPlan.COACH_YEARLY -> priceCoachYearly
        CheckoutPlan.LIFETIME -> priceLifetime
    }
}
