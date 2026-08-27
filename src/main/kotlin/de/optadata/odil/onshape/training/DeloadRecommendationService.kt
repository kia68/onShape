package de.optadata.odil.onshape.training

import de.optadata.odil.onshape.nutrition.FoodEntryRepository
import de.optadata.odil.onshape.onboarding.NutritionTargetRepository
import de.optadata.odil.onshape.security.RlsSession
import de.optadata.odil.onshape.trainlog.OneRepMaxCalculator
import de.optadata.odil.onshape.trainlog.WorkoutSessionRepository
import de.optadata.odil.onshape.trainlog.WorkoutSetRepository
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Service
import org.springframework.web.server.ResponseStatusException
import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import java.util.UUID

data class DeloadRecommendationResponse(
    val recommended: Boolean,
    val reasons: Set<DeloadReason>,
    val stagnantExerciseCount: Int,
    val rirMisses: Int,
    val rirComparisonsTotal: Int,
    val avgRecentPerceivedEffort: Double?,
    val weeksInCalorieDeficit: Int,
)

/**
 * FR-79 orchestriert [DeloadRecommendationDetector] mit echten Daten aus dem AKTIVEN Programm.
 * Reine Empfehlung -- veraendert nichts am Programm selbst (siehe Detector-KDoc). Kein
 * BIZ-01-Gate: "Deload-Automatik" kommt in der KONZEPT.md §15.1-Preistabelle gar nicht vor,
 * anders als Wochenbericht/Adaptives TDEE.
 */
@Service
class DeloadRecommendationService(
    private val programRepository: ProgramRepository,
    private val workoutSetRepository: WorkoutSetRepository,
    private val workoutSessionRepository: WorkoutSessionRepository,
    private val foodEntryRepository: FoodEntryRepository,
    private val nutritionTargetRepository: NutritionTargetRepository,
    private val rlsSession: RlsSession,
) {
    private companion object {
        const val STAGNATION_LOOKBACK_WEEKS = 3
        const val RIR_LOOKBACK_DAYS = 21L
        const val RECENT_SESSIONS_LIMIT = 3
        const val DEFICIT_LOOKBACK_WEEKS = 16L
        const val MIN_LOGGED_DAYS_PER_WEEK = 4
    }

    fun evaluate(userId: UUID, today: LocalDate = LocalDate.now()): DeloadRecommendationResponse {
        val program = rlsSession.asUser(userId) { programRepository.findActiveByUser(userId) }
            ?: throw ResponseStatusException(HttpStatus.NOT_FOUND, "Kein aktiver Plan")
        val exerciseIds = program.days.flatMap { it.items }.map { it.exerciseId }.distinct()

        val (stagnantCount, rirComparisons, recentSessions, target) = rlsSession.asUser(userId) {
            RawData(
                stagnantCount = exerciseIds.count { isStagnant(userId, it, today) },
                rirComparisons = workoutSetRepository.findRecentRirComparisons(userId, today.minusDays(RIR_LOOKBACK_DAYS).atStartOfDay(ZoneOffset.UTC).toInstant()),
                recentSessions = workoutSessionRepository.findHistory(userId, RECENT_SESSIONS_LIMIT),
                target = nutritionTargetRepository.findLatest(userId),
            )
        }

        val rirMisses = rirComparisons.count { it.actualRir > it.targetRir }
        val recentEfforts = recentSessions.mapNotNull { it.perceivedEffort }
        val avgRecentEffort = if (recentEfforts.isEmpty()) null else recentEfforts.average()
        val weeksInDeficit = rlsSession.asUser(userId) { countWeeksInDeficit(userId, target?.result?.kcal, today) }

        val input = DeloadInput(
            stagnantExerciseCount = stagnantCount,
            rirMisses = rirMisses,
            recentPerceivedEfforts = recentEfforts,
            weeksInCalorieDeficit = weeksInDeficit,
        )
        val reasons = DeloadRecommendationDetector.evaluate(input)

        return DeloadRecommendationResponse(
            recommended = reasons.isNotEmpty(),
            reasons = reasons,
            stagnantExerciseCount = stagnantCount,
            rirMisses = rirMisses,
            rirComparisonsTotal = rirComparisons.size,
            avgRecentPerceivedEffort = avgRecentEffort,
            weeksInCalorieDeficit = weeksInDeficit,
        )
    }

    /** "3 Wochen stagnierende Leistung": das geschaetzte 1RM ([OneRepMaxCalculator]) der
     * juengsten Trainingswoche ist NICHT hoeher als vor genau [STAGNATION_LOOKBACK_WEEKS] Wochen
     * -- braucht dafuer mindestens 3 Wochen MIT Daten (weniger Historie ist nicht beurteilbar,
     * zaehlt bewusst nicht als Stagnation). */
    private fun isStagnant(userId: UUID, exerciseId: UUID, today: LocalDate): Boolean {
        val weeklyBests = workoutSetRepository.findWorkingSetHistory(userId, exerciseId)
            .mapNotNull { (loggedAt, sample) ->
                val oneRm = OneRepMaxCalculator.estimate(sample.weightKg, sample.reps) ?: return@mapNotNull null
                weekStartOf(loggedAt) to oneRm
            }
            .groupBy({ it.first }, { it.second })
            .mapValues { it.value.max() }
            .toSortedMap()
        if (weeklyBests.size < STAGNATION_LOOKBACK_WEEKS) return false
        val recent = weeklyBests.values.toList().takeLast(STAGNATION_LOOKBACK_WEEKS)
        return recent.last() <= recent.first()
    }

    private fun weekStartOf(instant: Instant): LocalDate = instant.atZone(ZoneOffset.UTC).toLocalDate().with(DayOfWeek.MONDAY)

    /** ">8 Wochen im Kaloriendefizit": Interpretation = aufeinanderfolgende, VOLLSTAENDIG
     * abgeschlossene Wochen (die laufende Woche zaehlt nicht mit, um Verzerrung durch wenige
     * bisher geloggte Tage zu vermeiden) mit Ernaehrungs-Logging-Durchschnitt unter dem
     * aktuellen Tagesziel. Eine Woche mit weniger als [MIN_LOGGED_DAYS_PER_WEEK] geloggten Tagen
     * ist nicht verlaesslich beurteilbar und beendet die Zaehlung (keine Annahme in beide
     * Richtungen). */
    private fun countWeeksInDeficit(userId: UUID, targetKcal: Int?, today: LocalDate): Int {
        if (targetKcal == null) return 0
        val currentWeekStart = today.with(DayOfWeek.MONDAY)
        val from = currentWeekStart.minusWeeks(DEFICIT_LOOKBACK_WEEKS)
        val byWeek = foodEntryRepository.findDailyTotals(userId, from, currentWeekStart.minusDays(1))
            .groupBy { it.date.with(DayOfWeek.MONDAY) }

        var streak = 0
        var weekStart = currentWeekStart.minusWeeks(1)
        while (weekStart >= from) {
            val days = byWeek[weekStart].orEmpty()
            if (days.size < MIN_LOGGED_DAYS_PER_WEEK) break
            val avgKcal = days.sumOf { it.kcal } / days.size
            if (avgKcal >= targetKcal) break
            streak++
            weekStart = weekStart.minusWeeks(1)
        }
        return streak
    }

    private data class RawData(
        val stagnantCount: Int,
        val rirComparisons: List<de.optadata.odil.onshape.trainlog.RirComparison>,
        val recentSessions: List<de.optadata.odil.onshape.trainlog.WorkoutSession>,
        val target: de.optadata.odil.onshape.onboarding.StoredNutritionTarget?,
    )
}
