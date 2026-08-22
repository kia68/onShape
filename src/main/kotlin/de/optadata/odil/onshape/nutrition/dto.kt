package de.optadata.odil.onshape.nutrition

import jakarta.validation.constraints.DecimalMin
import jakarta.validation.constraints.Min
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotEmpty
import jakarta.validation.constraints.NotNull
import java.time.LocalDate
import java.util.UUID

// ---- Requests --------------------------------------------------------------------------

data class LogEntryApiRequest(
    val foodId: UUID?,
    val recipeId: UUID?,
    @field:NotNull val loggedDate: LocalDate,
    @field:NotBlank val slot: String,
    @field:DecimalMin("0.1") val grams: Double,
    val servingId: UUID?,
    @field:NotBlank val method: String,
    val clientId: String? = null,
)

data class CopyEntriesRequest(@field:NotNull val fromDate: LocalDate, @field:NotNull val toDate: LocalDate, val slot: String? = null)

data class RecipeItemApiRequest(@field:NotNull val foodId: UUID, @field:DecimalMin("0.1") val grams: Double)

data class CreateRecipeRequest(
    @field:NotBlank val name: String,
    @field:DecimalMin("1.0") val servings: Double,
    val instructions: String? = null,
    @field:NotEmpty val items: List<RecipeItemApiRequest>,
)

data class CreateSavedMealRequest(
    @field:NotBlank val name: String,
    @field:NotEmpty val items: List<RecipeItemApiRequest>,
)

data class LogSavedMealRequest(@field:NotNull val loggedDate: LocalDate, @field:NotBlank val slot: String)

data class WaterLogRequest(@field:NotNull val loggedDate: LocalDate, @field:Min(1) val amountMl: Int, val clientId: String? = null)

data class MeasurementRequest(
    @field:NotNull val measuredOn: LocalDate,
    val weightKg: Double? = null,
    val bodyFatPct: Double? = null,
    val waistCm: Double? = null,
    val hipCm: Double? = null,
    val chestCm: Double? = null,
    val armCm: Double? = null,
    val thighCm: Double? = null,
)

// ---- Responses --------------------------------------------------------------------------

data class ServingOptionResponse(val id: UUID, val label: String, val grams: Double, val isDefault: Boolean)

data class FoodSearchResultResponse(
    val id: UUID, val name: String, val brand: String?, val kcalPer100g: Double,
    val proteinGPer100g: Double, val fatGPer100g: Double, val carbsGPer100g: Double,
    val source: String, val trust: String, val servings: List<ServingOptionResponse>, val lastUsedGrams: Double?,
)

fun FoodSearchResult.toResponse() = FoodSearchResultResponse(
    id, name, brand, kcalPer100g, proteinGPer100g, fatGPer100g, carbsGPer100g,
    source.dbValue, trust.dbValue, servings.map { ServingOptionResponse(it.id, it.label, it.grams, it.isDefault) }, lastUsedGrams,
)

data class FoodEntryResponse(
    val id: UUID, val foodId: UUID?, val recipeId: UUID?, val loggedDate: LocalDate, val slot: String,
    val grams: Double, val method: String, val kcal: Double, val proteinG: Double, val fatG: Double,
    val carbsG: Double, val micros: Map<String, Double>, val clientId: String?, val name: String? = null,
)

fun FoodEntry.toResponse() = FoodEntryResponse(
    id, foodId, recipeId, loggedDate, slot.dbValue, grams, method.dbValue, kcal, proteinG, fatG, carbsG, micros, clientId,
)

/** FR-20: Tagesansicht-Eintraege tragen den (denormalisierten, siehe [FoodEntryWithName]) Namen mit. */
fun FoodEntryWithName.toResponse() = entry.toResponse().copy(name = name)

data class SlotSummaryResponse(val slot: String, val entries: List<FoodEntryResponse>, val kcal: Double, val proteinG: Double, val fatG: Double, val carbsG: Double)

data class DayViewResponse(
    val date: LocalDate, val slots: List<SlotSummaryResponse>, val totalKcal: Double, val totalProteinG: Double,
    val totalFatG: Double, val totalCarbsG: Double, val totalMicros: Map<String, Double>, val waterMl: Int,
    val targetKcal: Int?, val targetProteinG: Int?, val targetFatG: Int?, val targetCarbsG: Int?, val targetWaterMl: Int?,
)

fun DayView.toResponse() = DayViewResponse(
    date = date,
    slots = slots.map { SlotSummaryResponse(it.slot.dbValue, it.entries.map { e -> e.toResponse() }, it.kcal, it.proteinG, it.fatG, it.carbsG) },
    totalKcal = totalKcal, totalProteinG = totalProteinG, totalFatG = totalFatG, totalCarbsG = totalCarbsG,
    totalMicros = totalMicros, waterMl = waterMl, targetWaterMl = target?.waterMl,
    targetKcal = target?.kcal, targetProteinG = target?.proteinG, targetFatG = target?.fatG, targetCarbsG = target?.carbsG,
)

data class SavedMealItemResponse(val foodId: UUID, val grams: Double)
data class SavedMealResponse(val id: UUID, val name: String, val items: List<SavedMealItemResponse>)

fun SavedMeal.toResponse() = SavedMealResponse(id, name, items.map { SavedMealItemResponse(it.foodId, it.grams) })

data class RecipeResponse(
    val id: UUID, val name: String, val servings: Double, val instructions: String?, val isPublic: Boolean,
    val items: List<SavedMealItemResponse>, val perServingKcal: Double, val perServingProteinG: Double,
    val perServingFatG: Double, val perServingCarbsG: Double,
)

fun RecipeWithNutrition.toResponse() = RecipeResponse(
    recipe.id, recipe.name, recipe.servings, recipe.instructions, recipe.isPublic,
    recipe.items.map { SavedMealItemResponse(it.foodId, it.grams) },
    nutrition.perServingKcal, nutrition.perServingProteinG, nutrition.perServingFatG, nutrition.perServingCarbsG,
)

data class WaterEntryResponse(val id: UUID, val loggedDate: LocalDate, val amountMl: Int, val clientId: String?)

fun WaterEntry.toResponse() = WaterEntryResponse(id, loggedDate, amountMl, clientId)

data class WaterDayResponse(val date: LocalDate, val totalMl: Int, val entries: List<WaterEntryResponse>)
