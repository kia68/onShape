package de.optadata.odil.onshape.foodimport

/**
 * Normalisierte Zwischenform, die jeder Quellen-Client (BLS/USDA/OFF/...) liefern muss,
 * bevor Plausibilitaetspruefung, Deduplizierung und Import in `foods` laufen.
 * Naehrwerte sind immer pro 100 g bzw. 100 ml angegeben, siehe V2__foods.sql.
 */
data class ImportedFood(
    val source: FoodSource,
    val sourceId: String?,
    val barcode: String?,
    val brand: String?,
    val nameDe: String,
    val nameEn: String,
    val category: String? = null,
    val novaGroup: Int? = null,
    val nutriscore: Char? = null,
    val kcal: Double,
    val proteinG: Double,
    val fatG: Double,
    val saturatedFatG: Double? = null,
    val transFatG: Double? = null,
    val carbsG: Double,
    val sugarG: Double? = null,
    val fiberG: Double? = null,
    val saltG: Double? = null,
    val micros: Map<String, Double> = emptyMap(),
    val allergens: List<String> = emptyList(),
    val additives: List<String> = emptyList(),
    val isLiquid: Boolean = false,
    val servings: List<ImportedServing> = emptyList(),
)

data class ImportedServing(
    val labelDe: String,
    val labelEn: String,
    val grams: Double,
    val isDefault: Boolean = false,
)
