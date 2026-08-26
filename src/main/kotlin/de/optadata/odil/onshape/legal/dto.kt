package de.optadata.odil.onshape.legal

import jakarta.validation.constraints.NotNull
import java.time.Instant

/** Ein Schritt fuer alle fuenf Zwecke auf einmal (§14.1: "Eigener Schritt im Onboarding"). */
data class ConsentsRequest(
    @field:NotNull val core: Boolean,
    @field:NotNull val photoAi: Boolean,
    @field:NotNull val wearableSync: Boolean,
    @field:NotNull val analytics: Boolean,
    @field:NotNull val marketing: Boolean,
)

data class SingleConsentRequest(@field:NotNull val granted: Boolean)

data class ConsentResponse(val purpose: String, val granted: Boolean, val updatedAt: Instant?)

fun List<Consent>.toResponse(): List<ConsentResponse> =
    map { ConsentResponse(purpose = it.purpose.dbValue, granted = it.granted, updatedAt = it.updatedAt) }
