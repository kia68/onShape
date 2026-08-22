package de.optadata.odil.onshape.barcode

import jakarta.validation.constraints.DecimalMin
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotNull
import java.time.LocalDate
import java.util.UUID

// ---- Requests --------------------------------------------------------------------------

data class BarcodeScanRequest(@field:NotBlank val barcode: String, @field:NotNull val date: LocalDate)

data class ManualProductRequest(
    @field:NotBlank val barcode: String,
    @field:NotBlank val nameDe: String,
    val nameEn: String? = null,
    val brand: String? = null,
    val category: String? = null,
    @field:DecimalMin("0.0") val kcal: Double,
    @field:DecimalMin("0.0") val proteinG: Double,
    @field:DecimalMin("0.0") val fatG: Double,
    @field:DecimalMin("0.0") val carbsG: Double,
    val sugarG: Double? = null,
    val fiberG: Double? = null,
    val saltG: Double? = null,
    val allergens: List<String> = emptyList(),
    val isLiquid: Boolean = false,
)

// ---- Responses --------------------------------------------------------------------------

data class ReasonResponse(val code: String, val params: Map<String, Any>, val weight: Double)

fun FitScoreReason.toResponse() = ReasonResponse(code, params, weight)

data class ProductSummaryResponse(
    val id: UUID, val barcode: String?, val brand: String?, val name: String, val category: String?,
    val novaGroup: Int?, val nutriscore: Char?, val kcalPer100g: Double, val proteinGPer100g: Double,
    val fatGPer100g: Double, val carbsGPer100g: Double, val sugarGPer100g: Double?, val fiberGPer100g: Double?,
    val saltGPer100g: Double?, val allergens: List<String>, val additives: List<String>, val trust: String,
    val defaultServingGrams: Double,
)

fun FoodDetails.toResponse() = ProductSummaryResponse(
    id, barcode, brand, name, category, novaGroup, nutriscore, kcalPer100g, proteinGPer100g, fatGPer100g,
    carbsGPer100g, sugarGPer100g, fiberGPer100g, saltGPer100g, allergens, additives, trust.dbValue, defaultServingGrams,
)

data class AlternativeResponse(val product: ProductSummaryResponse, val score: Int, val reasons: List<ReasonResponse>)

fun AlternativeProduct.toResponse() = AlternativeResponse(food.toResponse(), fitScore.score, fitScore.reasons.map { it.toResponse() })

data class BarcodeScanResponse(
    val found: Boolean,
    val barcode: String,
    val product: ProductSummaryResponse?,
    val score: Int?,
    val allergenMatches: List<String>,
    val dietaryPreferenceConflict: String?,
    val reasons: List<ReasonResponse>,
    val alternatives: List<AlternativeResponse>,
)

fun BarcodeScanOutcome.toResponse(): BarcodeScanResponse = when (this) {
    is BarcodeScanOutcome.NotFound -> BarcodeScanResponse(
        found = false, barcode = barcode, product = null, score = null,
        allergenMatches = emptyList(), dietaryPreferenceConflict = null, reasons = emptyList(), alternatives = emptyList(),
    )
    is BarcodeScanOutcome.Found -> BarcodeScanResponse(
        found = true,
        barcode = product.barcode ?: "",
        product = product.toResponse(),
        score = fitScore.score,
        allergenMatches = fitScore.allergenMatches,
        dietaryPreferenceConflict = fitScore.dietaryPreferenceConflict,
        reasons = fitScore.reasons.map { it.toResponse() },
        alternatives = alternatives.map { it.toResponse() },
    )
}

data class ManualProductResponse(val id: UUID)
