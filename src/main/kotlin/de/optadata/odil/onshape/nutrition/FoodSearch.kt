package de.optadata.odil.onshape.nutrition

import de.optadata.odil.onshape.foodimport.TrustLevel
import java.util.UUID

data class ServingOption(val id: UUID, val label: String, val grams: Double, val isDefault: Boolean)

/** Nutrition NFR-14: jedes Ergebnis traegt Quelle/Vertrauenslevel sichtbar mit. */
data class FoodSearchResult(
    val id: UUID,
    val name: String,
    val brand: String?,
    val kcalPer100g: Double,
    val proteinGPer100g: Double,
    val fatGPer100g: Double,
    val carbsGPer100g: Double,
    val trust: TrustLevel,
    val servings: List<ServingOption>,
    /** FR-22: Portionsgroesse vorbelegt mit der zuletzt genutzten Menge dieses Nutzers. */
    val lastUsedGrams: Double?,
)
