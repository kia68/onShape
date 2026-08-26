package de.optadata.odil.onshape.billing

import de.optadata.odil.onshape.security.RlsSession
import org.springframework.stereotype.Service
import java.time.Clock
import java.time.Instant
import java.util.UUID

/**
 * Keine Zeile in `subscriptions` = FREE (gleiches Muster wie `user_consents`, siehe V17-
 * Kommentar). Kein periodischer Ablauf-Job: ein Abo bleibt auf seinem bezahlten Tier, bis
 * Stripe tatsaechlich `customer.subscription.deleted` sendet (z. B. nach fehlgeschlagener
 * Zahlung UND Stripes eigenem Dunning-Prozess) -- dieselbe Begruendung wie schon bei FR-133/
 * LEGAL-12 (kein Job, der etwas "veraltet" neu berechnen muesste).
 */
@Service
class SubscriptionService(
    private val subscriptionRepository: SubscriptionRepository,
    private val rlsSession: RlsSession,
    private val clock: Clock = Clock.systemUTC(),
) {

    fun current(userId: UUID): Subscription? = rlsSession.asUser(userId) { subscriptionRepository.findByUserId(userId) }

    fun currentTier(userId: UUID): Tier = current(userId)?.takeIf { it.status == SubscriptionStatus.ACTIVE }?.tier ?: Tier.FREE

    /** `checkout.session.completed`: `userId` kommt aus Stripes `client_reference_id`, vom
     * Checkout-Erstellen mitgegeben (siehe StripeGateway) -- deshalb hier normale
     * `asUser`-RLS statt der webhook-weiten Policy. */
    fun grantFromCheckout(
        userId: UUID,
        plan: CheckoutPlan,
        stripeCustomerId: String,
        stripeSubscriptionId: String?,
        currentPeriodEnd: Instant?,
    ) {
        rlsSession.asUser(userId) {
            subscriptionRepository.upsert(
                userId = userId,
                tier = plan.tier,
                billingPeriod = plan.period,
                status = SubscriptionStatus.ACTIVE,
                isLifetime = plan.period == BillingPeriod.LIFETIME,
                stripeCustomerId = stripeCustomerId,
                stripeSubscriptionId = stripeSubscriptionId,
                currentPeriodEnd = currentPeriodEnd,
                at = Instant.now(clock),
            )
        }
    }

    /** `customer.subscription.deleted`: kein `userId` im Event verfuegbar, nur die Stripe-IDs
     * -- braucht die System-Policy (V17). */
    fun revokeByStripeSubscriptionId(stripeSubscriptionId: String) {
        rlsSession.asSystemLookup { subscriptionRepository.cancelByStripeSubscriptionId(stripeSubscriptionId, Instant.now(clock)) }
    }

    /** BIZ-03: Deckel ist global ueber ALLE Nutzer, nicht pro Account -- braucht die
     * System-Policy (V17). */
    fun lifetimeCapReached(cap: Int): Boolean = rlsSession.asSystemLookup { subscriptionRepository.countLifetime() } >= cap
}
