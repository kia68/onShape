package de.optadata.odil.onshape.wellbeing

import de.optadata.odil.onshape.nutrition.FoodEntryRepository
import de.optadata.odil.onshape.onboarding.NutritionTargetRepository
import de.optadata.odil.onshape.security.RlsSession
import de.optadata.odil.onshape.trainlog.WorkoutSessionRepository
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.stereotype.Service
import java.sql.Timestamp
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.time.temporal.ChronoUnit
import java.util.UUID

/**
 * LEGAL-12 (KONZEPT.md §14.5): Guardrail-Status wird bei jedem Aufruf LIVE berechnet, nicht
 * zwischengespeichert -- dieselbe Begruendung wie bei FR-133 (Volumen-Historie, siehe
 * [de.optadata.odil.onshape.progress.ProgressService]): ein periodischer Neuberechnungs-Job
 * existiert nicht, ein gecachter Wert waere sonst veraltet, genau dann wenn er am wichtigsten ist.
 */
@Service
class WellbeingService(
    private val foodEntryRepository: FoodEntryRepository,
    private val nutritionTargetRepository: NutritionTargetRepository,
    private val workoutSessionRepository: WorkoutSessionRepository,
    private val jdbcTemplate: JdbcTemplate,
    private val rlsSession: RlsSession,
    private val clock: Clock = Clock.systemUTC(),
) {

    fun guardrailStatus(userId: UUID): GuardrailStatusResponse {
        val today = LocalDate.now(clock)
        val now = Instant.now(clock)

        val flags = rlsSession.asUser(userId) {
            val dailyTotals = foodEntryRepository.findDailyTotals(userId, today.minusDays(6), today)
            val target = nutritionTargetRepository.findLatest(userId)
            val kcalHistory = nutritionTargetRepository.findKcalHistorySince(userId, now.minus(30, ChronoUnit.DAYS))
            val sessionCount = workoutSessionRepository.countStartedSince(userId, now.minus(7, ChronoUnit.DAYS))

            WellbeingPatternDetector.evaluate(
                WellbeingInput(
                    loggedDailyKcal = dailyTotals.map { it.kcal },
                    targetKcal = target?.result?.kcal,
                    kcalHistoryChronological = kcalHistory.map { it.kcal },
                    trainingSessionsLast7Days = sessionCount,
                ),
            )
        }

        return GuardrailStatusResponse(
            hideCalorieDisplay = flags.isNotEmpty(),
            flags = flags.map { it.name },
            resources = if (flags.isNotEmpty()) WELLBEING_RESOURCES else emptyList(),
        )
    }

    /** Null, wenn der Nutzer noch kein Profil hat (Pausenmodus setzt ein Onboarding voraus). */
    fun pauseStatus(userId: UUID): PauseStatusResponse? =
        rlsSession.asUser(userId) {
            jdbcTemplate.query(
                "SELECT tracking_paused, tracking_paused_at FROM profiles WHERE user_id = ?",
                { rs, _ -> PauseStatusResponse(rs.getBoolean("tracking_paused"), rs.getTimestamp("tracking_paused_at")?.toInstant()) },
                userId,
            ).firstOrNull()
        }

    /** §14.5 "Pausenmodus": ein Klick, keine Rueckgewinnungs-Kampagne (also bewusst KEINE
     * Benachrichtigung/E-Mail bei Aktivierung oder waehrend der Pause -- es gibt hier schlicht
     * keinen Code dafuer), Daten bleiben unveraendert erhalten (nur ein Flag, kein Loeschen). */
    fun setPaused(userId: UUID, paused: Boolean): PauseStatusResponse? {
        val pausedAt = if (paused) Timestamp.from(Instant.now(clock)) else null
        val updated = rlsSession.asUser(userId) {
            jdbcTemplate.update(
                "UPDATE profiles SET tracking_paused = ?, tracking_paused_at = ? WHERE user_id = ?",
                paused, pausedAt, userId,
            )
        }
        if (updated == 0) return null
        return pauseStatus(userId)
    }
}
