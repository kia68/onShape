package de.optadata.odil.onshape.integrations

import de.optadata.odil.onshape.security.RlsSession
import de.optadata.odil.onshape.training.ExerciseRepository
import de.optadata.odil.onshape.trainlog.WorkoutSessionRepository
import de.optadata.odil.onshape.trainlog.WorkoutSetRepository
import org.springframework.stereotype.Service
import java.util.UUID

/** FR-153 (CSV-Import von Hevy/Strong, MVP -- "Wechselhuerde senken ist ein Akquisekanal").
 * Orchestriert: Parsen (quellenspezifisch) -> Uebungsabgleich ([ExerciseMatcher]) -> Speichern
 * (idempotent ueber `client_id`, siehe [WorkoutSessionRepository.insertImported]).
 *
 * FR-150/151 (Apple Health/Google Fit, Garmin/Fitbit/Withings/Polar) und FR-152 (Strava)
 * erfordern OAuth-Zugangsdaten bzw. nativen Mobile-SDK-Zugriff, der in dieser Session nicht
 * verfuegbar ist -- zurueckgestellt (docs/progress.md). MyFitnessPal- und Yazio-Import (ebenfalls
 * unter FR-153 genannt) sind aus demselben Grund NICHT Teil dieser Klasse: MyFitnessPal, weil das
 * offizielle CSV-Spaltenformat trotz mehrfacher Recherche nicht verlaesslich verifizierbar war;
 * Yazio, weil die App offiziell gar keinen CSV-Export anbietet (nur PDF bzw. DSGVO-Auskunftsersuchen). */
@Service
class ImportService(
    private val exerciseRepository: ExerciseRepository,
    private val workoutSessionRepository: WorkoutSessionRepository,
    private val workoutSetRepository: WorkoutSetRepository,
    private val rlsSession: RlsSession,
) {
    fun importHevy(userId: UUID, csvText: String): ImportSummary = import(userId, HevyCsvParser.parse(csvText), "hevy")

    fun importStrong(userId: UUID, csvText: String): ImportSummary = import(userId, StrongCsvParser.parse(csvText), "strong")

    private fun import(userId: UUID, parsed: ParseResult, sourceTag: String): ImportSummary = rlsSession.asUser(userId) {
        val catalog = exerciseRepository.findCatalogForMatching()
        val unmatched = mutableMapOf<String, Int>()
        var sessionsImported = 0
        var setsImported = 0

        for ((sessionKey, rowsInSession) in parsed.rows.groupBy { it.sessionKey }) {
            val first = rowsInSession.first()
            val (session, sessionIsNew) = workoutSessionRepository.insertImported(
                userId = userId,
                startedAt = first.startedAt,
                finishedAt = first.finishedAt,
                notes = "Import: ${first.sessionTitle}",
                clientId = "import:$sourceTag:$sessionKey",
            )
            if (sessionIsNew) sessionsImported++

            for (row in rowsInSession) {
                val exerciseId = ExerciseMatcher.match(catalog, row.exerciseName)
                if (exerciseId == null) {
                    unmatched.merge(row.exerciseName, 1, Int::plus)
                    continue
                }
                val (_, setIsNew) = workoutSetRepository.insertImported(
                    sessionId = session.id,
                    exerciseId = exerciseId,
                    setIndex = row.setIndex,
                    weightKg = row.weightKg,
                    reps = row.reps,
                    durationSec = row.durationSec,
                    distanceM = row.distanceM,
                    loggedAt = row.startedAt.plusSeconds(row.setIndex.toLong()),
                    clientId = "import:$sourceTag:$sessionKey:${row.exerciseName}:${row.setIndex}",
                )
                if (setIsNew) setsImported++
            }
        }

        ImportSummary(sessionsImported, setsImported, unmatched, parsed.warnings)
    }
}
