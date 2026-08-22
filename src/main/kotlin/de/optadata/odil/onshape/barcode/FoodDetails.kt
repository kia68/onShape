package de.optadata.odil.onshape.barcode

import de.optadata.odil.onshape.foodimport.TrustLevel
import java.util.UUID

/** Volle Produktdaten fuer den Barcode-Scan (FR-40..FR-49) -- mehr Felder als
 * [de.optadata.odil.onshape.nutrition.FoodSearchResult], das nur fuers Quick-Add-Logging
 * gebraucht wird. */
data class FoodDetails(
    val id: UUID,
    val barcode: String?,
    val brand: String?,
    val name: String,
    val category: String?,
    val novaGroup: Int?,
    val nutriscore: Char?,
    val kcalPer100g: Double,
    val proteinGPer100g: Double,
    val fatGPer100g: Double,
    val saturatedFatGPer100g: Double?,
    val transFatGPer100g: Double?,
    val carbsGPer100g: Double,
    val sugarGPer100g: Double?,
    val fiberGPer100g: Double?,
    val saltGPer100g: Double?,
    val micros: Map<String, Double>,
    val allergens: List<String>,
    val additives: List<String>,
    val trust: TrustLevel,
    val defaultServingGrams: Double,
)
