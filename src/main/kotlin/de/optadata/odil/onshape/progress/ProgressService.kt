package de.optadata.odil.onshape.progress

import de.optadata.odil.onshape.nutrition.FoodEntryRepository
import de.optadata.odil.onshape.onboarding.BodyMeasurementRepository
import de.optadata.odil.onshape.onboarding.NutritionTargetRepository
import de.optadata.odil.onshape.onboarding.ProfileRepository
import de.optadata.odil.onshape.security.RlsSession
import de.optadata.odil.onshape.training.VolumeCorridor
import de.optadata.odil.onshape.trainlog.WorkoutSetRepository
import org.springframework.stereotype.Service
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.Period
import java.util.UUID

@Service
class ProgressService(
    private val bodyMeasurementRepository: BodyMeasurementRepository,
    private val foodEntryRepository: FoodEntryRepository,
    private val nutritionTargetRepository: NutritionTargetRepository,
    private val profileRepository: ProfileRepository,
    private val workoutSetRepository: WorkoutSetRepository,
    private val rlsSession: RlsSession,
) {

    /** FR-130. */
    fun weightHistory(userId: UUID, from: LocalDate, to: LocalDate): WeightHistoryResponse {
        val history = rlsSession.asUser(userId) { bodyMeasurementRepository.findHistory(userId, from, to) }
        val points = history.filter { it.weightKg != null }
            .map { DatedValue(it.measuredOn, it.weightKg!!) }
            .sortedBy { it.date }
        val smoothed = SevenDayMovingAverage.compute(points)
        return WeightHistoryResponse(
            raw = points.map { WeightPointResponse(it.date, it.value) },
            sevenDayAverage = smoothed.map { WeightPointResponse(it.date, it.value) },
        )
    }

    /** FR-131. */
    fun nutritionHistory(userId: UUID, from: LocalDate, to: LocalDate): NutritionHistoryResponse {
        val (daily, target) = rlsSession.asUser(userId) {
            foodEntryRepository.findDailyTotals(userId, from, to) to nutritionTargetRepository.findLatest(userId)
        }
        val adherence = AdherenceCalculator.rate(daily.map { it.date }.toSet(), from, to)
        val weeklyAverages = daily
            .groupBy { it.date.with(DayOfWeek.MONDAY) }
            .map { (weekStart, days) ->
                WeeklyNutritionAverageResponse(
                    weekStart = weekStart,
                    kcal = days.sumOf { it.kcal } / days.size,
                    proteinG = days.sumOf { it.proteinG } / days.size,
                    fatG = days.sumOf { it.fatG } / days.size,
                    carbsG = days.sumOf { it.carbsG } / days.size,
                )
            }
            .sortedBy { it.weekStart }
        return NutritionHistoryResponse(
            daily = daily.map { DailyNutritionResponse(it.date, it.kcal, it.proteinG, it.fatG, it.carbsG) },
            weeklyAverages = weeklyAverages,
            adherenceRate = adherence,
            targetKcal = target?.result?.kcal,
        )
    }

    /** FR-133: tatsaechlich geloggtes Volumen je Woche/Muskel, verglichen mit dem Zielkorridor
     * aus dem Profil (Erfahrung/Alter, siehe [VolumeCorridor]). */
    fun volumeHistory(userId: UUID, from: LocalDate, to: LocalDate, today: LocalDate = LocalDate.now()): List<WeeklyMuscleVolumeResponse> {
        val (profile, volume) = rlsSession.asUser(userId) {
            profileRepository.findByUserId(userId) to workoutSetRepository.findWeeklyMuscleVolume(userId, from, to)
        }
        val corridor = profile?.let { VolumeCorridor.forProfile(it.experience, Period.between(it.birthDate, today).years) }
        return volume.map {
            WeeklyMuscleVolumeResponse(it.weekStart, it.muscle, it.sets, corridor?.startSetsPerMuscle ?: 0, corridor?.maxSetsPerMuscle ?: 0)
        }
    }
}
