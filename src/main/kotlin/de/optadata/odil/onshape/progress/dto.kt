package de.optadata.odil.onshape.progress

import java.time.LocalDate

data class WeightPointResponse(val date: LocalDate, val weightKg: Double)

data class WeightHistoryResponse(val raw: List<WeightPointResponse>, val sevenDayAverage: List<WeightPointResponse>)

data class DailyNutritionResponse(val date: LocalDate, val kcal: Double, val proteinG: Double, val fatG: Double, val carbsG: Double)

data class WeeklyNutritionAverageResponse(val weekStart: LocalDate, val kcal: Double, val proteinG: Double, val fatG: Double, val carbsG: Double)

data class NutritionHistoryResponse(
    val daily: List<DailyNutritionResponse>,
    val weeklyAverages: List<WeeklyNutritionAverageResponse>,
    val adherenceRate: Double,
    val targetKcal: Int?,
)

data class WeeklyMuscleVolumeResponse(
    val weekStart: LocalDate,
    val muscle: String,
    val sets: Double,
    val corridorMin: Int,
    val corridorMax: Int,
)
