package de.optadata.odil.onshape.trainlog

import de.optadata.odil.onshape.security.RlsSession
import de.optadata.odil.onshape.training.Exercise
import de.optadata.odil.onshape.training.ExerciseRepository
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Service
import org.springframework.web.server.ResponseStatusException
import java.time.Instant
import java.util.UUID

data class LogSetInput(
    val exerciseId: UUID,
    val setIndex: Int,
    val weightKg: Double?,
    val reps: Int?,
    val durationSec: Int?,
    val distanceM: Double?,
    val rir: Int?,
    val isWarmup: Boolean,
    val completed: Boolean,
    val clientId: String?,
)

data class LogSetResult(val set: WorkoutSet, val personalRecords: List<PersonalRecord>)

data class OneRepMaxPoint(val loggedAt: Instant, val estimated1Rm: Double)

@Service
class WorkoutLogService(
    private val workoutSessionRepository: WorkoutSessionRepository,
    private val workoutSetRepository: WorkoutSetRepository,
    private val exerciseRepository: ExerciseRepository,
    private val rlsSession: RlsSession,
) {

    fun startSession(userId: UUID, programDayId: UUID?, clientId: String?): WorkoutSession =
        rlsSession.asUser(userId) { workoutSessionRepository.start(userId, programDayId, clientId) }

    /** FR-96: der aktive (noch nicht beendete) Workout, falls vorhanden -- traegt den Wake-Lock-
     * / Offline-Modus im Frontend. */
    fun activeSession(userId: UUID): WorkoutSession =
        rlsSession.asUser(userId) { workoutSessionRepository.findActiveByUser(userId) }
            ?: throw ResponseStatusException(HttpStatus.NOT_FOUND, "Kein laufendes Workout")

    fun sessionDetail(userId: UUID, sessionId: UUID): Pair<WorkoutSession, List<WorkoutSet>> = rlsSession.asUser(userId) {
        val session = workoutSessionRepository.findById(sessionId)?.takeIf { it.userId == userId }
            ?: throw ResponseStatusException(HttpStatus.NOT_FOUND, "Workout nicht gefunden")
        session to workoutSetRepository.findBySession(sessionId)
    }

    fun finishSession(userId: UUID, sessionId: UUID, perceivedEffort: Int?, notes: String?): WorkoutSession =
        rlsSession.asUser(userId) { workoutSessionRepository.finish(userId, sessionId, perceivedEffort, notes) }
            ?: throw ResponseStatusException(HttpStatus.NOT_FOUND, "Kein laufendes Workout mit dieser ID")

    fun history(userId: UUID, limit: Int): List<WorkoutSession> =
        rlsSession.asUser(userId) { workoutSessionRepository.findHistory(userId, limit) }

    /** FR-90/91: Vorbelegung fuer den naechsten Satz dieser Uebung. Bezieht sich immer auf die
     * VORHERIGE Session -- innerhalb der laufenden Session bereits geloggte Saetze derselben
     * Uebung zaehlen nicht als "letztes Mal". FR-94: Aufwaermsaetze werden aus dem fuer HEUTE
     * vorgeschlagenen Arbeitsgewicht berechnet (nicht aus dem letzten Mal) -- faellt auf das
     * zuletzt geloggte Gewicht zurueck, wenn die Progression kein neues vorschlaegt (z.B. beim
     * allerersten Satz dieser Uebung ueberhaupt gibt es auch dafuer noch nichts). */
    fun prefill(userId: UUID, exerciseId: UUID, repMax: Int?, targetRir: Int?): PrefillSuggestion = rlsSession.asUser(userId) {
        val active = workoutSessionRepository.findActiveByUser(userId)
        val last = active?.let { workoutSetRepository.findLastWorkingSet(userId, exerciseId, it.id) }
        val lastValues = last?.let { LastSetValues(it.weightKg, it.reps, it.rir) }
        val suggestion = ProgressionSuggester.suggest(lastValues, repMax, targetRir)
        val warmupSets = WarmupSetCalculator.calculate(suggestion.suggestedWeightKg ?: suggestion.lastWeightKg)
        suggestion.copy(warmupSets = warmupSets)
    }

    /** FR-90 (2 Taps), FR-93 (RIR), FR-96 (Offline via clientId), FR-98 (PR-Erkennung inline
     * in der Antwort, damit das Frontend sofort feiern kann, ohne einen zweiten Request). */
    fun logSet(userId: UUID, sessionId: UUID, input: LogSetInput): LogSetResult = rlsSession.asUser(userId) {
        workoutSessionRepository.findById(sessionId)?.takeIf { it.userId == userId }
            ?: throw ResponseStatusException(HttpStatus.NOT_FOUND, "Workout nicht gefunden")

        val priorSamples = if (input.weightKg != null && input.reps != null) {
            workoutSetRepository.findWorkingSetHistory(userId, input.exerciseId).map { it.second }
        } else {
            emptyList()
        }
        val prior = input.weightKg?.let { PersonalRecordDetector.priorBestsFrom(priorSamples, it) } ?: PriorBests()

        val set = workoutSetRepository.insert(
            sessionId, input.exerciseId, input.setIndex, input.weightKg, input.reps, input.durationSec,
            input.distanceM, input.rir, input.isWarmup, input.completed, input.clientId,
        )
        val records = PersonalRecordDetector.detect(LoggedSet(set.weightKg, set.reps, set.isWarmup, set.completed), prior)
        LogSetResult(set, records)
    }

    /** FR-97: Verlaufskurve, ein Punkt pro geloggtem Arbeitssatz. */
    fun oneRepMaxHistory(userId: UUID, exerciseId: UUID): List<OneRepMaxPoint> = rlsSession.asUser(userId) {
        workoutSetRepository.findWorkingSetHistory(userId, exerciseId).mapNotNull { (loggedAt, sample) ->
            OneRepMaxCalculator.estimate(sample.weightKg, sample.reps)?.let { OneRepMaxPoint(loggedAt, it) }
        }
    }

    /** FR-98 als Uebersicht (nicht im Log-Moment): aktuelle Bestwerte unabhaengig von einem
     * bestimmten neuen Satzgewicht -- der Wiederholungsrekord bezieht sich deshalb auf "irgendein
     * Gewicht" (Schwelle 0.0), nicht auf ein konkretes bevorstehendes Gewicht wie in [logSet]. */
    fun personalBests(userId: UUID, exerciseId: UUID): PriorBests = rlsSession.asUser(userId) {
        val history = workoutSetRepository.findWorkingSetHistory(userId, exerciseId).map { it.second }
        PersonalRecordDetector.priorBestsFrom(history, newWeightKg = 0.0)
    }

    /** FR-132: Uebungsauswahl fuer den Fortschritts-Screen -- nur Uebungen, zu denen es
     * ueberhaupt eine Verlaufskurve geben kann. */
    fun loggedExercises(userId: UUID): List<Exercise> {
        val loggedIds = rlsSession.asUser(userId) { workoutSetRepository.findLoggedExerciseIds(userId) }.toSet()
        return exerciseRepository.findAll().filter { it.id in loggedIds }
    }
}
