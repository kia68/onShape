package de.optadata.odil.onshape.nutrition

import de.optadata.odil.onshape.security.RlsSession
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Service
import org.springframework.web.server.ResponseStatusException
import tools.jackson.databind.ObjectMapper
import java.time.LocalDate
import java.util.UUID

data class LogFoodRequest(
    val foodId: UUID?,
    val recipeId: UUID?,
    val loggedDate: LocalDate,
    val slot: MealSlot,
    val grams: Double,
    val servingId: UUID?,
    val method: EntryMethod,
    val clientId: String?,
)

/** FR-22/23/24/25/26/31: Eintraege loggen, kopieren, aus Meals/Rezepten uebernehmen. Jede
 * oeffentliche Methode kapselt ihren eigenen RLS-Kontext (siehe [RlsSession]) -- Aufrufer
 * (Controller) muessen sich darum nicht kuemmern. */
@Service
class NutritionEntryService(
    private val foodEntryRepository: FoodEntryRepository,
    private val foodSearchRepository: FoodSearchRepository,
    private val recipeRepository: RecipeRepository,
    private val savedMealRepository: SavedMealRepository,
    private val rlsSession: RlsSession,
    private val objectMapper: ObjectMapper,
) {

    fun log(userId: UUID, request: LogFoodRequest): FoodEntry = rlsSession.asUser(userId) {
        val newEntry = when {
            request.foodId != null -> fromFood(request)
            request.recipeId != null -> fromRecipe(request)
            else -> throw ResponseStatusException(HttpStatus.BAD_REQUEST, "foodId oder recipeId erforderlich")
        }
        foodEntryRepository.insert(userId, newEntry)
    }

    /** FR-23: Multi-Select -- mehrere Eintraege in einem Rutsch. */
    fun logBatch(userId: UUID, requests: List<LogFoodRequest>): List<FoodEntry> =
        requests.map { log(userId, it) }

    fun delete(userId: UUID, id: UUID) {
        val deleted = rlsSession.asUser(userId) { foodEntryRepository.delete(userId, id) }
        if (!deleted) throw ResponseStatusException(HttpStatus.NOT_FOUND, "Eintrag nicht gefunden")
    }

    /** FR-24: Tag oder einzelne Mahlzeit kopieren. */
    fun copy(userId: UUID, fromDate: LocalDate, toDate: LocalDate, slot: MealSlot?): List<FoodEntry> =
        rlsSession.asUser(userId) {
            val source = foodEntryRepository.findByDate(userId, fromDate).filter { slot == null || it.slot == slot }
            source.map { entry ->
                foodEntryRepository.insert(
                    userId,
                    NewFoodEntry(
                        foodId = entry.foodId, recipeId = entry.recipeId, loggedDate = toDate, slot = entry.slot,
                        grams = entry.grams, servingId = entry.servingId, method = EntryMethod.COPY,
                        kcal = entry.kcal, proteinG = entry.proteinG, fatG = entry.fatG, carbsG = entry.carbsG,
                        micros = entry.micros, clientId = null,
                    ),
                )
            }
        }

    /** FR-25: alle Positionen eines gespeicherten Meals auf einmal verbuchen. */
    fun logSavedMeal(userId: UUID, savedMealId: UUID, loggedDate: LocalDate, slot: MealSlot): List<FoodEntry> =
        rlsSession.asUser(userId) {
            val meal = savedMealRepository.findById(userId, savedMealId)
                ?: throw ResponseStatusException(HttpStatus.NOT_FOUND, "Meal nicht gefunden")
            meal.items.map { item ->
                val nutrition = foodSearchRepository.findById(item.foodId)
                    ?: error("Lebensmittel ${item.foodId} aus gespeichertem Meal nicht mehr vorhanden")
                foodEntryRepository.insert(userId, buildFoodEntry(item.foodId, null, loggedDate, slot, item.grams, null, EntryMethod.QUICK_ADD, nutrition, null))
            }
        }

    private fun fromFood(request: LogFoodRequest): NewFoodEntry {
        val nutrition = foodSearchRepository.findById(request.foodId!!)
            ?: throw ResponseStatusException(HttpStatus.NOT_FOUND, "Lebensmittel nicht gefunden")
        return buildFoodEntry(request.foodId, null, request.loggedDate, request.slot, request.grams, request.servingId, request.method, nutrition, request.clientId)
    }

    private fun fromRecipe(request: LogFoodRequest): NewFoodEntry {
        val recipe = recipeRepository.findById(request.recipeId!!)
            ?: throw ResponseStatusException(HttpStatus.NOT_FOUND, "Rezept nicht gefunden")
        val nutritionByFood = recipe.items.associate { it.foodId to (foodSearchRepository.findById(it.foodId) ?: error("Zutat ${it.foodId} nicht gefunden")) }
        val recipeNutrition = RecipeNutritionCalculator.calculate(recipe.items, nutritionByFood, recipe.servings, objectMapper)
        val gramsConsumed = request.grams
        return NewFoodEntry(
            foodId = null, recipeId = request.recipeId, loggedDate = request.loggedDate, slot = request.slot,
            grams = request.grams, servingId = null, method = EntryMethod.RECIPE,
            kcal = recipeNutrition.kcalPerGram * gramsConsumed, proteinG = recipeNutrition.proteinGPerGram * gramsConsumed,
            fatG = recipeNutrition.fatGPerGram * gramsConsumed, carbsG = recipeNutrition.carbsGPerGram * gramsConsumed,
            micros = recipeNutrition.microsPerGram.mapValues { (_, perGram) -> perGram * gramsConsumed },
            clientId = request.clientId,
        )
    }

    private fun buildFoodEntry(
        foodId: UUID, recipeId: UUID?, loggedDate: LocalDate, slot: MealSlot, grams: Double,
        servingId: UUID?, method: EntryMethod, nutrition: FoodNutrition, clientId: String?,
    ): NewFoodEntry {
        val factor = grams / 100.0
        return NewFoodEntry(
            foodId = foodId, recipeId = recipeId, loggedDate = loggedDate, slot = slot, grams = grams,
            servingId = servingId, method = method,
            kcal = nutrition.kcalPer100g * factor, proteinG = nutrition.proteinGPer100g * factor,
            fatG = nutrition.fatGPer100g * factor, carbsG = nutrition.carbsGPer100g * factor,
            micros = MicroNutrients.scale(MicroNutrients.parse(objectMapper, nutrition.microsPer100g), grams),
            clientId = clientId,
        )
    }
}
