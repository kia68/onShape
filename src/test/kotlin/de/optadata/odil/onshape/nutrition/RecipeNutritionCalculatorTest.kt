package de.optadata.odil.onshape.nutrition

import tools.jackson.databind.json.JsonMapper
import java.util.UUID
import kotlin.test.Test
import kotlin.test.assertEquals

class RecipeNutritionCalculatorTest {

    private val objectMapper = JsonMapper.builder().build()

    private fun nutrition(id: UUID, kcal: Double, protein: Double, fat: Double, carbs: Double, micros: String = "{}") =
        FoodNutrition(id, kcal, protein, fat, carbs, micros)

    @Test
    fun `summiert zutaten proportional zur grammzahl`() {
        val oats = UUID.randomUUID()
        val milk = UUID.randomUUID()
        val items = listOf(RecipeItem(oats, 100.0), RecipeItem(milk, 200.0))
        val byFood = mapOf(
            oats to nutrition(oats, kcal = 370.0, protein = 13.0, fat = 7.0, carbs = 61.0),
            milk to nutrition(milk, kcal = 42.0, protein = 3.4, fat = 1.0, carbs = 5.0),
        )

        val result = RecipeNutritionCalculator.calculate(items, byFood, servings = 2.0, objectMapper = objectMapper)

        // oats: 370 kcal fuer 100g -> voll; milk: 42 kcal/100g * 200g = 84 kcal
        assertEquals(370.0 + 84.0, result.totalKcal, 0.001)
        assertEquals(13.0 + 3.4 * 2, result.totalProteinG, 0.001)
        assertEquals(300.0, result.totalGrams, 0.001)
        assertEquals(2.0, result.servings)
    }

    @Test
    fun `pro portion ist gesamt geteilt durch anzahl portionen`() {
        val food = UUID.randomUUID()
        val items = listOf(RecipeItem(food, 400.0))
        val byFood = mapOf(food to nutrition(food, kcal = 200.0, protein = 20.0, fat = 8.0, carbs = 10.0))

        val result = RecipeNutritionCalculator.calculate(items, byFood, servings = 4.0, objectMapper = objectMapper)

        // 400g bei 200kcal/100g = 800 kcal gesamt, / 4 Portionen = 200 kcal/Portion
        assertEquals(200.0, result.perServingKcal, 0.001)
    }

    @Test
    fun `pro gramm ist gesamt geteilt durch gesamtgewicht fuer food_entries-logging`() {
        val food = UUID.randomUUID()
        val items = listOf(RecipeItem(food, 500.0))
        val byFood = mapOf(food to nutrition(food, kcal = 100.0, protein = 10.0, fat = 5.0, carbs = 5.0))

        val result = RecipeNutritionCalculator.calculate(items, byFood, servings = 1.0, objectMapper = objectMapper)

        // 500g bei 100kcal/100g = 500 kcal gesamt / 500g = 1 kcal/g
        assertEquals(1.0, result.kcalPerGram, 0.001)
    }

    @Test
    fun `summiert mikronaehrstoffe ueber alle zutaten`() {
        val a = UUID.randomUUID()
        val b = UUID.randomUUID()
        val items = listOf(RecipeItem(a, 100.0), RecipeItem(b, 100.0))
        val byFood = mapOf(
            a to nutrition(a, 100.0, 1.0, 1.0, 1.0, micros = """{"iron_mg":2.0}"""),
            b to nutrition(b, 100.0, 1.0, 1.0, 1.0, micros = """{"iron_mg":1.0,"zinc_mg":0.5}"""),
        )

        val result = RecipeNutritionCalculator.calculate(items, byFood, servings = 1.0, objectMapper = objectMapper)

        assertEquals(3.0, result.totalMicros["iron_mg"])
        assertEquals(0.5, result.totalMicros["zinc_mg"])
    }

    @Test
    fun `unbekannte zutat wirft eine klare fehlermeldung`() {
        val missing = UUID.randomUUID()
        val items = listOf(RecipeItem(missing, 100.0))
        val exception = kotlin.test.assertFailsWith<IllegalStateException> {
            RecipeNutritionCalculator.calculate(items, emptyMap(), servings = 1.0, objectMapper = objectMapper)
        }
        kotlin.test.assertTrue(exception.message!!.contains(missing.toString()))
    }
}
