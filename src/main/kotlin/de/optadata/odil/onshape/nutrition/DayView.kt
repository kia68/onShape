package de.optadata.odil.onshape.nutrition

import de.optadata.odil.onshape.onboarding.NutritionTargetResult
import java.time.LocalDate

data class SlotSummary(val slot: MealSlot, val entries: List<FoodEntryWithName>, val kcal: Double, val proteinG: Double, val fatG: Double, val carbsG: Double)

/** FR-20: Tagesansicht mit Kalorien/Makros/Restbudget. FR-28: Mikronaehrstoff-Summe des Tages. */
data class DayView(
    val date: LocalDate,
    val slots: List<SlotSummary>,
    val totalKcal: Double,
    val totalProteinG: Double,
    val totalFatG: Double,
    val totalCarbsG: Double,
    val totalMicros: Map<String, Double>,
    val waterMl: Int,
    val target: NutritionTargetResult?,
)
