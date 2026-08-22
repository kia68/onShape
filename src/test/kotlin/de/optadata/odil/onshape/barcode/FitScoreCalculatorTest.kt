package de.optadata.odil.onshape.barcode

import de.optadata.odil.onshape.foodimport.TrustLevel
import de.optadata.odil.onshape.onboarding.Goal
import java.util.UUID
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class FitScoreCalculatorTest {

    private fun food(
        kcal: Double = 100.0,
        protein: Double = 5.0,
        fat: Double = 5.0,
        carbs: Double = 10.0,
        sugar: Double? = 2.0,
        fiber: Double? = 2.0,
        transFat: Double? = null,
        nova: Int? = 2,
        allergens: List<String> = emptyList(),
        micros: Map<String, Double> = emptyMap(),
        category: String? = "snacks",
        servingGrams: Double = 100.0,
    ) = FoodDetails(
        id = UUID.randomUUID(), barcode = "4000000000000", brand = "Testmarke", name = "Testprodukt",
        category = category, novaGroup = nova, nutriscore = 'B', kcalPer100g = kcal, proteinGPer100g = protein,
        fatGPer100g = fat, saturatedFatGPer100g = null, transFatGPer100g = transFat, carbsGPer100g = carbs,
        sugarGPer100g = sugar, fiberGPer100g = fiber, saltGPer100g = 1.0, micros = micros, allergens = allergens,
        additives = emptyList(), trust = TrustLevel.COMMUNITY, defaultServingGrams = servingGrams,
    )

    private fun profile(goal: Goal = Goal.MAINTAIN, dietaryPrefs: List<String> = emptyList(), allergens: List<String> = emptyList()) =
        FitScoreProfile(goal, dietaryPrefs, allergens)

    private fun emptyDay() = FitScoreDayContext(null, null, null, null, 0.0, 0.0, 0.0, 0.0)

    private fun dayWithTarget(kcal: Int = 2000, protein: Int = 150, carbs: Int = 200, fat: Int = 60, consumedKcal: Double = 0.0, consumedProtein: Double = 0.0) =
        FitScoreDayContext(kcal, protein, carbs, fat, consumedKcal, consumedProtein, 0.0, 0.0)

    @Test
    fun `allergen im nutzerprofil erzwingt score 0`() {
        val result = FitScoreCalculator.calculate(food(allergens = listOf("milk")), profile(allergens = listOf("milk")), emptyDay())
        assertEquals(0, result.score)
        assertEquals(listOf("milk"), result.allergenMatches)
    }

    @Test
    fun `allergen match ist case-insensitiv`() {
        val result = FitScoreCalculator.calculate(food(allergens = listOf("Milk")), profile(allergens = listOf("MILK")), emptyDay())
        assertEquals(0, result.score)
    }

    @Test
    fun `allergen ausserhalb des profils blockiert nicht`() {
        val result = FitScoreCalculator.calculate(food(allergens = listOf("gluten")), profile(allergens = listOf("milk")), emptyDay())
        assertTrue(result.score > 0)
        assertTrue(result.allergenMatches.isEmpty())
    }

    @Test
    fun `vegane praeferenz wird durch milch-allergen-tag verletzt`() {
        val result = FitScoreCalculator.calculate(food(allergens = listOf("milk")), profile(dietaryPrefs = listOf("vegan")), emptyDay())
        assertEquals(0, result.score)
        assertEquals("vegan", result.dietaryPreferenceConflict)
    }

    @Test
    fun `vegetarische praeferenz wird durch fisch verletzt aber nicht durch milch`() {
        val vegetarian = profile(dietaryPrefs = listOf("vegetarian"))
        val withFish = FitScoreCalculator.calculate(food(allergens = listOf("fish")), vegetarian, emptyDay())
        val withMilk = FitScoreCalculator.calculate(food(allergens = listOf("milk")), vegetarian, emptyDay())
        assertEquals("vegetarian", withFish.dietaryPreferenceConflict)
        assertEquals(null, withMilk.dietaryPreferenceConflict)
    }

    @Test
    fun `abnehmen bevorzugt niedrige energiedichte`() {
        val lowDensity = FitScoreCalculator.calculate(food(kcal = 50.0), profile(goal = Goal.LOSE), emptyDay())
        val highDensity = FitScoreCalculator.calculate(food(kcal = 550.0), profile(goal = Goal.LOSE), emptyDay())
        assertTrue(lowDensity.score > highDensity.score)
    }

    @Test
    fun `muskelaufbau bevorzugt hohe energiedichte`() {
        val lowDensity = FitScoreCalculator.calculate(food(kcal = 50.0), profile(goal = Goal.GAIN_MUSCLE), emptyDay())
        val highDensity = FitScoreCalculator.calculate(food(kcal = 550.0), profile(goal = Goal.GAIN_MUSCLE), emptyDay())
        assertTrue(highDensity.score > lowDensity.score)
    }

    @Test
    fun `portion die das restbudget sprengt senkt den score`() {
        val fitsInBudget = FitScoreCalculator.calculate(food(kcal = 100.0), profile(), dayWithTarget(kcal = 2000, consumedKcal = 0.0))
        val exceedsBudget = FitScoreCalculator.calculate(food(kcal = 100.0), profile(), dayWithTarget(kcal = 2000, consumedKcal = 1990.0))
        assertTrue(fitsInBudget.score > exceedsBudget.score)
    }

    @Test
    fun `hoher proteingehalt bei proteindefizit erhoeht den score`() {
        val highProteinFood = food(protein = 30.0)
        val withDeficit = FitScoreCalculator.calculate(highProteinFood, profile(), dayWithTarget(protein = 150, consumedProtein = 10.0))
        val withoutDeficit = FitScoreCalculator.calculate(highProteinFood, profile(), dayWithTarget(protein = 150, consumedProtein = 149.0))
        assertTrue(withDeficit.score > withoutDeficit.score)
    }

    @Test
    fun `ohne tagesziel ist die makro-komponente neutral und crasht nicht`() {
        val result = FitScoreCalculator.calculate(food(), profile(), emptyDay())
        assertTrue(result.score in 0..100)
    }

    @Test
    fun `nova1 bekommt einen besseren score als nova4`() {
        val nova1 = FitScoreCalculator.calculate(food(nova = 1), profile(), emptyDay())
        val nova4 = FitScoreCalculator.calculate(food(nova = 4), profile(), emptyDay())
        assertTrue(nova1.score > nova4.score)
    }

    @Test
    fun `fehlende nova-klassifikation ist neutral statt fehlerhaft`() {
        val result = FitScoreCalculator.calculate(food(nova = null), profile(), emptyDay())
        assertTrue(result.score in 0..100)
    }

    @Test
    fun `hohe saettigung protein und ballaststoffe bei wenig kalorien schlaegt leere kalorien`() {
        val satiating = food(kcal = 120.0, protein = 25.0, fiber = 8.0)
        val empty = food(kcal = 400.0, protein = 1.0, fiber = 0.0)
        val satiatingResult = FitScoreCalculator.calculate(satiating, profile(), emptyDay())
        val emptyResult = FitScoreCalculator.calculate(empty, profile(), emptyDay())
        assertTrue(satiatingResult.score > emptyResult.score)
    }

    @Test
    fun `mikronaehrstoffe erhoehen die naehrstoffdichte gegenueber leerer angabe`() {
        val withMicros = food(micros = mapOf("iron_mg" to 5.0, "vitamin_c_mg" to 40.0))
        val withoutMicros = food(micros = emptyMap())
        val withMicrosResult = FitScoreCalculator.calculate(withMicros, profile(), emptyDay())
        val withoutMicrosResult = FitScoreCalculator.calculate(withoutMicros, profile(), emptyDay())
        assertTrue(withMicrosResult.score >= withoutMicrosResult.score)
    }

    @Test
    fun `mehr als 90 prozent des zuckerbudgets in einer portion senkt den score deutlich`() {
        // Tagesziel 2000 kcal -> Zuckerbudget = 2000*0.10/4 = 50g. 46g Zucker in einer Portion = 92%.
        val highSugar = food(sugar = 46.0, kcal = 200.0)
        val lowSugar = food(sugar = 2.0, kcal = 200.0)
        val highSugarResult = FitScoreCalculator.calculate(highSugar, profile(), dayWithTarget(kcal = 2000))
        val lowSugarResult = FitScoreCalculator.calculate(lowSugar, profile(), dayWithTarget(kcal = 2000))
        assertTrue(highSugarResult.reasons.any { it.code == "high_sugar_share" })
        assertTrue(lowSugarResult.reasons.none { it.code == "high_sugar_share" })
        assertTrue(highSugarResult.score < lowSugarResult.score)
    }

    @Test
    fun `transfette senken den score und werden als grund gemeldet`() {
        val withTransFat = FitScoreCalculator.calculate(food(transFat = 1.5), profile(), emptyDay())
        val withoutTransFat = FitScoreCalculator.calculate(food(transFat = null), profile(), emptyDay())
        assertTrue(withTransFat.reasons.any { it.code == "trans_fat_present" })
        assertTrue(withTransFat.score < withoutTransFat.score)
    }

    @Test
    fun `score bleibt immer im bereich 0 bis 100`() {
        val worstCase = food(kcal = 600.0, sugar = 100.0, transFat = 5.0, nova = 4, protein = 0.0, fiber = 0.0)
        val result = FitScoreCalculator.calculate(worstCase, profile(goal = Goal.LOSE), dayWithTarget(kcal = 1200, consumedKcal = 1190.0))
        assertTrue(result.score in 0..100)
    }

    @Test
    fun `breakdown enthaelt alle komponenten fuer die erklaerbarkeit`() {
        val result = FitScoreCalculator.calculate(food(), profile(), dayWithTarget())
        for (key in listOf("goalFit", "nutrientDensity", "macroContribution", "processing", "satiety", "baseScore", "servingGrams")) {
            assertTrue(result.breakdown.containsKey(key), "breakdown sollte '$key' enthalten")
        }
    }

    @Test
    fun `gruende sind absteigend nach gewicht sortiert`() {
        val result = FitScoreCalculator.calculate(food(sugar = 46.0, kcal = 200.0), profile(), dayWithTarget(kcal = 2000))
        val weights = result.reasons.map { it.weight }
        assertEquals(weights.sortedDescending(), weights)
    }
}
