package de.optadata.odil.onshape.progress

import de.optadata.odil.onshape.billing.SubscriptionService
import de.optadata.odil.onshape.billing.TierPolicy
import de.optadata.odil.onshape.nutrition.FoodEntryRepository
import de.optadata.odil.onshape.onboarding.BodyMeasurementRepository
import de.optadata.odil.onshape.onboarding.NutritionTargetRepository
import de.optadata.odil.onshape.onboarding.ProfileRepository
import de.optadata.odil.onshape.security.RlsSession
import de.optadata.odil.onshape.training.VolumeCorridor
import de.optadata.odil.onshape.trainlog.WorkoutSessionRepository
import de.optadata.odil.onshape.trainlog.WorkoutSetRepository
import org.springframework.stereotype.Service
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.Period
import java.time.ZoneOffset
import java.util.UUID

@Service
class ProgressService(
    private val bodyMeasurementRepository: BodyMeasurementRepository,
    private val foodEntryRepository: FoodEntryRepository,
    private val nutritionTargetRepository: NutritionTargetRepository,
    private val profileRepository: ProfileRepository,
    private val workoutSetRepository: WorkoutSetRepository,
    private val workoutSessionRepository: WorkoutSessionRepository,
    private val subscriptionService: SubscriptionService,
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
     * aus dem Profil (Erfahrung/Alter, siehe [VolumeCorridor]). BIZ-01 (§15.1 "Volumen-
     * Analytics: Basis" im Free-Tier): `from` wird auf die letzten N Wochen VOR HEUTE geklemmt
     * (nicht relativ zum angefragten `to` -- ein Free-Nutzer sieht ein rollendes Fenster der
     * juengsten Vergangenheit, unabhaengig davon, welchen Bereich das Frontend anfragt), siehe
     * [TierPolicy.volumeHistoryWindowWeeks]. */
    fun volumeHistory(userId: UUID, from: LocalDate, to: LocalDate, today: LocalDate = LocalDate.now()): List<WeeklyMuscleVolumeResponse> {
        val windowWeeks = TierPolicy.volumeHistoryWindowWeeks(subscriptionService.currentTier(userId))
        val clampedFrom = if (windowWeeks != null) maxOf(from, today.minusWeeks(windowWeeks.toLong())) else from

        val (profile, volume) = rlsSession.asUser(userId) {
            profileRepository.findByUserId(userId) to workoutSetRepository.findWeeklyMuscleVolume(userId, clampedFrom, to)
        }
        val corridor = profile?.let { VolumeCorridor.forProfile(it.experience, Period.between(it.birthDate, today).years) }
        return volume.map {
            WeeklyMuscleVolumeResponse(it.weekStart, it.muscle, it.sets, corridor?.startSetsPerMuscle ?: 0, corridor?.maxSetsPerMuscle ?: 0)
        }
    }

    /** FR-135. BIZ-01: Free-Tier hat gar keinen Zugriff (siehe [TierPolicy.canShowWeeklyReport]),
     * anders als die anderen Fortschritts-Endpunkte, die nur eingeschraenkt sind. [weekStart]
     * MUSS ein Montag sein (gleiche Wochendefinition wie die `weeklyAverages`-Gruppierung in
     * [nutritionHistory]) -- der Aufrufer (Controller) klemmt darauf. */
    fun weeklyReport(userId: UUID, weekStart: LocalDate): WeeklyReportResponse {
        if (!TierPolicy.canShowWeeklyReport(subscriptionService.currentTier(userId))) {
            throw WeeklyReportRequiresUpgradeException()
        }
        val weekEnd = weekStart.plusDays(6)
        val weekStartInstant = weekStart.atStartOfDay(ZoneOffset.UTC).toInstant()
        val weekEndExclusiveInstant = weekEnd.plusDays(1).atStartOfDay(ZoneOffset.UTC).toInstant()

        val (profile, daily, target, sessionsCompleted, weightHistory) = rlsSession.asUser(userId) {
            WeeklyReportRawData(
                profile = profileRepository.findByUserId(userId),
                daily = foodEntryRepository.findDailyTotals(userId, weekStart, weekEnd),
                target = nutritionTargetRepository.findLatest(userId),
                sessionsCompleted = workoutSessionRepository.countStartedBetween(userId, weekStartInstant, weekEndExclusiveInstant),
                weightHistory = bodyMeasurementRepository.findHistory(userId, weekStart, weekEnd),
            )
        }

        val daysLogged = daily.map { it.date }.toSet().size
        val avgKcal = if (daily.isEmpty()) null else daily.sumOf { it.kcal } / daily.size
        // findHistory liefert DESC (neueste zuerst) -- fuer eine chronologische Differenz
        // muss hier aufsteigend sortiert werden, sonst kehrt sich das Vorzeichen um.
        val weights = weightHistory.sortedBy { it.measuredOn }.mapNotNull { it.weightKg }
        val weightChangeKg = if (weights.size >= 2) weights.last() - weights.first() else null

        val input = WeeklyReportInput(
            sessionsCompleted = sessionsCompleted,
            sessionsPlanned = profile?.trainingDaysWeek ?: 0,
            nutritionDaysLogged = daysLogged,
            avgKcal = avgKcal,
            targetKcal = target?.result?.kcal,
        )
        val result = WeeklyReportGenerator.generate(input)

        return WeeklyReportResponse(
            weekStart = weekStart,
            weekEnd = weekEnd,
            sessionsCompleted = sessionsCompleted,
            sessionsPlanned = input.sessionsPlanned,
            nutritionDaysLogged = daysLogged,
            avgKcal = avgKcal,
            targetKcal = input.targetKcal,
            weightChangeKg = weightChangeKg,
            trainingRating = result.trainingRating,
            nutritionLoggingRating = result.nutritionLoggingRating,
            nutritionTargetRating = result.nutritionTargetRating,
            recommendation = result.recommendation,
        )
    }

    private data class WeeklyReportRawData(
        val profile: de.optadata.odil.onshape.onboarding.Profile?,
        val daily: List<de.optadata.odil.onshape.nutrition.DailyNutritionTotal>,
        val target: de.optadata.odil.onshape.onboarding.StoredNutritionTarget?,
        val sessionsCompleted: Int,
        val weightHistory: List<de.optadata.odil.onshape.onboarding.BodyMeasurement>,
    )
}
