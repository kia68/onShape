package de.optadata.odil.onshape.nutrition

import java.util.UUID

data class RecipeItemInput(val foodId: UUID, val grams: Double)

data class Recipe(
    val id: UUID,
    val userId: UUID?,
    val name: String,
    val servings: Double,
    val instructions: String?,
    val sourceUrl: String?,
    val isPublic: Boolean,
    val items: List<RecipeItem>,
)

data class RecipeItem(val foodId: UUID, val grams: Double)

/** Gesamtnaehrwerte eines Rezepts (Summe der Zutaten), Basis fuer FR-26 ("Naehrwerte pro
 * Portion, skalierbar") und fuer das Verbuchen eines Rezepts als `food_entries`-Zeile
 * (Naehrwert pro Gramm, konsistent zum Lebensmittel-Logging). */
data class RecipeNutrition(
    val totalGrams: Double,
    val totalKcal: Double,
    val totalProteinG: Double,
    val totalFatG: Double,
    val totalCarbsG: Double,
    val totalMicros: Map<String, Double>,
    val servings: Double,
) {
    val perServingKcal get() = totalKcal / servings
    val perServingProteinG get() = totalProteinG / servings
    val perServingFatG get() = totalFatG / servings
    val perServingCarbsG get() = totalCarbsG / servings

    val kcalPerGram get() = totalKcal / totalGrams
    val proteinGPerGram get() = totalProteinG / totalGrams
    val fatGPerGram get() = totalFatG / totalGrams
    val carbsGPerGram get() = totalCarbsG / totalGrams
    val microsPerGram get() = totalMicros.mapValues { (_, total) -> total / totalGrams }
}

object RecipeNutritionCalculator {
    fun calculate(items: List<RecipeItem>, nutritionByFoodId: Map<UUID, FoodNutrition>, servings: Double, objectMapper: tools.jackson.databind.ObjectMapper): RecipeNutrition {
        var kcal = 0.0
        var protein = 0.0
        var fat = 0.0
        var carbs = 0.0
        var grams = 0.0
        val microsList = mutableListOf<Map<String, Double>>()
        for (item in items) {
            val nutrition = nutritionByFoodId[item.foodId] ?: error("Unbekanntes Lebensmittel ${item.foodId} in Rezeptzutaten")
            val factor = item.grams / 100.0
            kcal += nutrition.kcalPer100g * factor
            protein += nutrition.proteinGPer100g * factor
            fat += nutrition.fatGPer100g * factor
            carbs += nutrition.carbsGPer100g * factor
            grams += item.grams
            microsList += MicroNutrients.scale(MicroNutrients.parse(objectMapper, nutrition.microsPer100g), item.grams)
        }
        return RecipeNutrition(
            totalGrams = grams,
            totalKcal = kcal,
            totalProteinG = protein,
            totalFatG = fat,
            totalCarbsG = carbs,
            totalMicros = MicroNutrients.sum(microsList),
            servings = servings,
        )
    }
}
