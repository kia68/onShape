package de.optadata.odil.onshape.billing

import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.stereotype.Repository
import java.sql.ResultSet
import java.sql.Timestamp
import java.time.Instant
import java.util.UUID

/** `findByUserId`/`upsert` muessen innerhalb von `RlsSession.asUser(userId) { ... }` laufen
 * (owner_only-Policy). `findByStripeCustomerId`/`updateFromWebhook`/`cancelByStripeCustomerId`
 * sind fuer den Webhook-Codepfad ohne Nutzerkontext und muessen innerhalb von
 * `RlsSession.asWebhookLookup { ... }` laufen (webhook_lookup-Policy, V17). */
@Repository
class SubscriptionRepository(private val jdbcTemplate: JdbcTemplate) {

    fun findByUserId(userId: UUID): Subscription? =
        jdbcTemplate.query(SELECT_SQL, { rs, _ -> rs.toSubscription() }, userId).firstOrNull()

    /** BIZ-03: wie viele Nutzer bereits den Lifetime-Deal haben, fuer den Deckel aus §15.1. */
    fun countLifetime(): Int =
        jdbcTemplate.queryForObject("SELECT count(*) FROM subscriptions WHERE is_lifetime", Int::class.java) ?: 0

    /** `checkout.session.completed`: legt die Zeile an oder ersetzt sie vollstaendig -- ein
     * neuer Kauf ist immer der neue Wahrheitsstand, kein partielles Update. */
    fun upsert(
        userId: UUID,
        tier: Tier,
        billingPeriod: BillingPeriod,
        status: SubscriptionStatus,
        isLifetime: Boolean,
        stripeCustomerId: String?,
        stripeSubscriptionId: String?,
        currentPeriodEnd: Instant?,
        at: Instant,
    ) {
        jdbcTemplate.update(
            """
            INSERT INTO subscriptions (user_id, tier, billing_period, status, is_lifetime, stripe_customer_id, stripe_subscription_id, current_period_end, updated_at)
            VALUES (?, ?::tier_t, ?::billing_period_t, ?::subscription_status_t, ?, ?, ?, ?, ?)
            ON CONFLICT (user_id) DO UPDATE SET
                tier = EXCLUDED.tier, billing_period = EXCLUDED.billing_period, status = EXCLUDED.status,
                is_lifetime = EXCLUDED.is_lifetime, stripe_customer_id = EXCLUDED.stripe_customer_id,
                stripe_subscription_id = EXCLUDED.stripe_subscription_id, current_period_end = EXCLUDED.current_period_end,
                updated_at = EXCLUDED.updated_at
            """.trimIndent(),
            userId, tier.dbValue, billingPeriod.dbValue, status.dbValue, isLifetime,
            stripeCustomerId, stripeSubscriptionId,
            currentPeriodEnd?.let { Timestamp.from(it) }, Timestamp.from(at),
        )
    }

    /** `customer.subscription.deleted`: faellt auf FREE zurueck (kein Grace-Period-Job, siehe
     * SubscriptionService-KDoc). Lifetime-Abos werden nie geloescht -- Stripe kennt sie nur als
     * abgeschlossene Einmalzahlung, es gibt kein `customer.subscription`-Objekt dafuer. */
    fun cancelByStripeSubscriptionId(stripeSubscriptionId: String, at: Instant): Int =
        jdbcTemplate.update(
            "UPDATE subscriptions SET tier = 'free'::tier_t, status = 'canceled'::subscription_status_t, updated_at = ? WHERE stripe_subscription_id = ?",
            Timestamp.from(at), stripeSubscriptionId,
        )

    private fun ResultSet.toSubscription() = Subscription(
        userId = getObject("user_id", UUID::class.java),
        tier = Tier.entries.first { it.dbValue == getString("tier") },
        billingPeriod = BillingPeriod.entries.first { it.dbValue == getString("billing_period") },
        status = SubscriptionStatus.entries.first { it.dbValue == getString("status") },
        isLifetime = getBoolean("is_lifetime"),
        stripeCustomerId = getString("stripe_customer_id"),
        stripeSubscriptionId = getString("stripe_subscription_id"),
        currentPeriodEnd = getTimestamp("current_period_end")?.toInstant(),
        updatedAt = getTimestamp("updated_at").toInstant(),
    )

    private companion object {
        const val SELECT_SQL = """
            SELECT user_id, tier, billing_period, status, is_lifetime, stripe_customer_id, stripe_subscription_id, current_period_end, updated_at
            FROM subscriptions WHERE user_id = ?
        """
    }
}
