package de.optadata.odil.onshape.billing

import java.time.Instant
import java.util.UUID

data class Subscription(
    val userId: UUID,
    val tier: Tier,
    val billingPeriod: BillingPeriod,
    val status: SubscriptionStatus,
    val isLifetime: Boolean,
    val stripeCustomerId: String?,
    val stripeSubscriptionId: String?,
    val currentPeriodEnd: Instant?,
    val updatedAt: Instant,
)

class BillingNotConfiguredException :
    RuntimeException("Stripe ist nicht konfiguriert (stripe.secret-key fehlt)")

class LifetimeCapReachedException(cap: Int) :
    RuntimeException("Lifetime-Deal-Kontingent erschoepft (Limit: $cap)")

class NoStripeCustomerException :
    RuntimeException("Kein Stripe-Kunde fuer dieses Konto -- noch kein Kauf abgeschlossen")

class InvalidWebhookSignatureException :
    RuntimeException("Stripe-Webhook-Signatur ungueltig")
