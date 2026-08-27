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

data class WeeklyReportResponse(
    val weekStart: LocalDate,
    val weekEnd: LocalDate,
    val sessionsCompleted: Int,
    val sessionsPlanned: Int,
    val nutritionDaysLogged: Int,
    val avgKcal: Double?,
    val targetKcal: Int?,
    /** Informativ, NICHT Teil der Bewertung -- siehe WeeklyReportGenerator-KDoc. */
    val weightChangeKg: Double?,
    val trainingRating: Rating,
    val nutritionLoggingRating: Rating,
    val nutritionTargetRating: Rating?,
    val recommendation: WeeklyReportRecommendation,
)

/** BIZ-01 (§15.1 "Wochenbericht: —" im Free-Tier), siehe [de.optadata.odil.onshape.billing.TierPolicy.canShowWeeklyReport]. */
class WeeklyReportRequiresUpgradeException :
    RuntimeException("Wochenbericht ist ein Plus/Coach-Feature -- auf Plus/Coach upgraden")
