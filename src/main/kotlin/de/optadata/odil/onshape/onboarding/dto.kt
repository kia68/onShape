package de.optadata.odil.onshape.onboarding

import jakarta.validation.constraints.DecimalMax
import jakarta.validation.constraints.DecimalMin
import jakarta.validation.constraints.Max
import jakarta.validation.constraints.Min
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotNull
import jakarta.validation.constraints.Past
import java.time.LocalDate

/** FR-02..FR-09 in einem kombinierten Request -- das Onboarding ist ein einziger Schritt
 * (FR-10: <=90 Sekunden, "jederzeit abbrechbar mit Defaults" wird clientseitig geloest, indem
 * das Frontend fehlende Felder mit denselben Defaults wie die DB-Spalten vorbelegt). */
data class OnboardingRequest(
    @field:NotBlank val sex: String,
    @field:NotNull @field:Past val birthDate: LocalDate,
    @field:DecimalMin("100.0") @field:DecimalMax("250.0") val heightCm: Double,
    @field:DecimalMin("20.0") @field:DecimalMax("400.0") val weightKg: Double,
    @field:DecimalMin("3.0") @field:DecimalMax("70.0") val bodyFatPct: Double? = null,
    @field:NotBlank val experience: String,
    @field:DecimalMin("1.10") @field:DecimalMax("2.00") val activityPal: Double,
    @field:NotBlank val goal: String,
    @field:DecimalMin("0.0") @field:DecimalMax("2.0") val goalRatePctWeek: Double,
    @field:DecimalMin("30.0") @field:DecimalMax("400.0") val targetWeightKg: Double? = null,
    val dietaryPrefs: List<String> = emptyList(),
    val allergens: List<String> = emptyList(),
    val injuries: List<String> = emptyList(),
    val equipment: List<String> = emptyList(),
    @field:Min(1) @field:Max(7) val trainingDaysWeek: Int,
    @field:Min(10) @field:Max(240) val sessionMinutes: Int,
    @field:NotNull val healthScreening: HealthScreeningAnswers,
)

data class OnboardingResultResponse(
    val kcal: Int,
    val proteinG: Int,
    val fatG: Int,
    val carbsG: Int,
    val fiberG: Int,
    val waterMl: Int,
    val calculation: Map<String, Any?>,
    val healthAdvisory: HealthScreeningResult,
)

/** FR-134. `eligible=false` liefert nur [reason] und den formelbasierten [formulaTdeeKcal] als
 * Fallback -- kein [adaptiveTdeeKcal] (KONZEPT.md: "sonst ... erklaert, warum"). */
data class AdaptiveTdeeResponse(
    val eligible: Boolean,
    val reason: AdaptiveTdeeIneligibleReason?,
    val adaptiveTdeeKcal: Int?,
    val formulaTdeeKcal: Int,
    val windowDays: Int,
    val weighInsInWindow: Int,
    val nutritionAdherence: Double,
)

/** BIZ-01 (§15.1 "Adaptives TDEE: —" im Free-Tier), siehe [de.optadata.odil.onshape.billing.TierPolicy.canShowAdaptiveTdee]. */
class AdaptiveTdeeRequiresUpgradeException :
    RuntimeException("Adaptives TDEE ist ein Plus/Coach-Feature -- auf Plus/Coach upgraden")
