package de.optadata.odil.onshape.billing

import jakarta.validation.constraints.NotBlank
import java.time.Instant

data class SubscriptionResponse(
    val tier: String,
    val billingPeriod: String?,
    val status: String?,
    val isLifetime: Boolean,
    val currentPeriodEnd: Instant?,
)

fun Subscription?.toResponse(): SubscriptionResponse =
    if (this == null) SubscriptionResponse(Tier.FREE.dbValue, null, null, false, null)
    else SubscriptionResponse(tier.dbValue, billingPeriod.dbValue, status.dbValue, isLifetime, currentPeriodEnd)

data class CheckoutRequest(@field:NotBlank val plan: String)

data class CheckoutResponse(val checkoutUrl: String)

data class BillingPortalResponse(val portalUrl: String)
