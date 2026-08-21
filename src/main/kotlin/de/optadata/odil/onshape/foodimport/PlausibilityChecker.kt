package de.optadata.odil.onshape.foodimport

import kotlin.math.abs

enum class PlausibilityFlag {
    ATWATER_MISMATCH,
    MACRO_SUM_EXCEEDS_100G,
    ZERO_NUTRIENTS,
    CATEGORY_OUTLIER,
}

data class PlausibilityResult(val flags: List<PlausibilityFlag>) {
    val isPlausible: Boolean get() = flags.isEmpty()
}

/** Statistik einer Lebensmittelkategorie fuer den Ausreisser-Check, extern per Query ermittelt. */
data class CategoryStats(val meanKcal: Double, val stdDevKcal: Double)

/**
 * Automatische Plausibilitaetspruefung aus KONZEPT.md §10.4 Schritt 5.
 * Reine, DB-/IO-freie Regeln — bewusst so gehalten, damit sie ohne Fixtures testbar bleiben.
 */
object PlausibilityChecker {

    private const val ATWATER_TOLERANCE = 0.15
    private const val MACRO_SUM_LIMIT_G = 100.0
    private const val OUTLIER_SIGMA_THRESHOLD = 3.0

    fun atwaterCheck(kcal: Double, proteinG: Double, fatG: Double, carbsG: Double): Boolean {
        val estimated = 4 * proteinG + 9 * fatG + 4 * carbsG
        if (estimated == 0.0) return kcal == 0.0
        val deviation = abs(kcal - estimated) / estimated
        return deviation <= ATWATER_TOLERANCE
    }

    fun macroSumCheck(proteinG: Double, fatG: Double, carbsG: Double, fiberG: Double?): Boolean =
        (proteinG + fatG + carbsG + (fiberG ?: 0.0)) <= MACRO_SUM_LIMIT_G

    fun zeroNutrientCheck(kcal: Double, proteinG: Double, fatG: Double, carbsG: Double): Boolean =
        !(kcal == 0.0 && proteinG == 0.0 && fatG == 0.0 && carbsG == 0.0)

    fun outlierCheck(kcal: Double, stats: CategoryStats?): Boolean {
        if (stats == null || stats.stdDevKcal == 0.0) return true
        val sigma = abs(kcal - stats.meanKcal) / stats.stdDevKcal
        return sigma <= OUTLIER_SIGMA_THRESHOLD
    }

    fun evaluate(food: ImportedFood, categoryStats: CategoryStats? = null): PlausibilityResult {
        val flags = buildList {
            if (!atwaterCheck(food.kcal, food.proteinG, food.fatG, food.carbsG)) {
                add(PlausibilityFlag.ATWATER_MISMATCH)
            }
            if (!macroSumCheck(food.proteinG, food.fatG, food.carbsG, food.fiberG)) {
                add(PlausibilityFlag.MACRO_SUM_EXCEEDS_100G)
            }
            if (!zeroNutrientCheck(food.kcal, food.proteinG, food.fatG, food.carbsG)) {
                add(PlausibilityFlag.ZERO_NUTRIENTS)
            }
            if (!outlierCheck(food.kcal, categoryStats)) {
                add(PlausibilityFlag.CATEGORY_OUTLIER)
            }
        }
        return PlausibilityResult(flags)
    }
}
