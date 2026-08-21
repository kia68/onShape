package de.optadata.odil.onshape.foodimport

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class PlausibilityCheckerTest {

    private fun food(
        kcal: Double,
        proteinG: Double = 0.0,
        fatG: Double = 0.0,
        carbsG: Double = 0.0,
        fiberG: Double? = null,
    ) = ImportedFood(
        source = FoodSource.OFF,
        sourceId = "1",
        barcode = null,
        brand = null,
        nameDe = "Test",
        nameEn = "Test",
        kcal = kcal,
        proteinG = proteinG,
        fatG = fatG,
        carbsG = carbsG,
        fiberG = fiberG,
    )

    @Test
    fun `atwater check passes within tolerance`() {
        // 10g protein + 10g fat + 10g carbs -> 4*10+9*10+4*10 = 170 kcal
        assertTrue(PlausibilityChecker.atwaterCheck(kcal = 175.0, proteinG = 10.0, fatG = 10.0, carbsG = 10.0))
    }

    @Test
    fun `atwater check flags implausible kcal`() {
        assertFalse(PlausibilityChecker.atwaterCheck(kcal = 500.0, proteinG = 10.0, fatG = 10.0, carbsG = 10.0))
    }

    @Test
    fun `macro sum over 100g is flagged`() {
        assertFalse(PlausibilityChecker.macroSumCheck(proteinG = 40.0, fatG = 40.0, carbsG = 40.0, fiberG = null))
    }

    @Test
    fun `macro sum at or under 100g is fine`() {
        assertTrue(PlausibilityChecker.macroSumCheck(proteinG = 30.0, fatG = 30.0, carbsG = 30.0, fiberG = 5.0))
    }

    @Test
    fun `all-zero nutrients on an edible product is flagged`() {
        assertFalse(PlausibilityChecker.zeroNutrientCheck(kcal = 0.0, proteinG = 0.0, fatG = 0.0, carbsG = 0.0))
    }

    @Test
    fun `outlier check passes without category stats`() {
        assertTrue(PlausibilityChecker.outlierCheck(kcal = 900.0, stats = null))
    }

    @Test
    fun `outlier check flags beyond 3 sigma`() {
        val stats = CategoryStats(meanKcal = 250.0, stdDevKcal = 50.0)
        assertFalse(PlausibilityChecker.outlierCheck(kcal = 900.0, stats = stats))
        assertTrue(PlausibilityChecker.outlierCheck(kcal = 300.0, stats = stats))
    }

    @Test
    fun `evaluate combines all flags`() {
        val implausible = food(kcal = 0.0, proteinG = 0.0, fatG = 0.0, carbsG = 0.0)
        val result = PlausibilityChecker.evaluate(implausible)
        assertFalse(result.isPlausible)
        assertTrue(PlausibilityFlag.ZERO_NUTRIENTS in result.flags)

        val plausible = food(kcal = 250.0, proteinG = 5.0, fatG = 10.0, carbsG = 40.0, fiberG = 3.0)
        assertEquals(true, PlausibilityChecker.evaluate(plausible).isPlausible)
    }
}
