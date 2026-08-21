package de.optadata.odil.onshape.nutrition

import de.optadata.odil.onshape.security.RlsSession
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Service
import org.springframework.web.server.ResponseStatusException
import tools.jackson.databind.ObjectMapper
import java.util.UUID

data class RecipeWithNutrition(val recipe: Recipe, val nutrition: RecipeNutrition)

/** FR-26. */
@Service
class RecipeService(
    private val recipeRepository: RecipeRepository,
    private val foodSearchRepository: FoodSearchRepository,
    private val rlsSession: RlsSession,
    private val objectMapper: ObjectMapper,
) {

    fun create(userId: UUID, name: String, servings: Double, instructions: String?, items: List<RecipeItemInput>): RecipeWithNutrition {
        require(items.isNotEmpty()) { "Rezept braucht mindestens eine Zutat" }
        val id = rlsSession.asUser(userId) { recipeRepository.insert(userId, name, servings, instructions, items) }
        return get(userId, id)
    }

    fun get(userId: UUID, id: UUID): RecipeWithNutrition {
        val recipe = rlsSession.asUser(userId) { recipeRepository.findById(id) }
            ?: throw ResponseStatusException(HttpStatus.NOT_FOUND, "Rezept nicht gefunden")
        return RecipeWithNutrition(recipe, computeNutrition(recipe))
    }

    fun listOwnAndPublic(userId: UUID): List<RecipeWithNutrition> {
        val recipes = rlsSession.asUser(userId) { recipeRepository.findOwnAndPublic(userId) }
        return recipes.map { RecipeWithNutrition(it, computeNutrition(it)) }
    }

    private fun computeNutrition(recipe: Recipe): RecipeNutrition {
        val nutritionByFood = recipe.items.associate {
            it.foodId to (foodSearchRepository.findById(it.foodId) ?: error("Zutat ${it.foodId} nicht gefunden"))
        }
        return RecipeNutritionCalculator.calculate(recipe.items, nutritionByFood, recipe.servings, objectMapper)
    }
}
