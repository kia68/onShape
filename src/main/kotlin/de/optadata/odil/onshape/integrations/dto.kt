package de.optadata.odil.onshape.integrations

import java.time.Instant

/** Quellen-neutrale Zwischendarstellung EINES geloggten Satzes aus einer importierten Datei --
 * [HevyCsvParser] und [StrongCsvParser] uebersetzen ihr jeweiliges Format hierhin, damit
 * [ImportService] quellenunabhaengig bleibt. `sessionKey` gruppiert Zeilen zu Sessions (z.B.
 * `"<start_time>|<title>"`), unabhaengig vom spaeteren client_id-Format in der DB. */
data class ParsedWorkoutRow(
    val sessionKey: String,
    val sessionTitle: String,
    val startedAt: Instant,
    val finishedAt: Instant?,
    val exerciseName: String,
    val setIndex: Int,
    val weightKg: Double?,
    val reps: Int?,
    val durationSec: Int?,
    val distanceM: Double?,
)

/** Ergebnis eines reinen Parse-Schritts: geparste Zeilen plus menschenlesbare Warnungen ueber
 * uebersprungene/nicht eindeutig interpretierbare Daten (z.B. Zeilen ohne Uebungsname, oder
 * Einheiten-Mehrdeutigkeiten bei Strong) -- nichts wird still verworfen. */
data class ParseResult(val rows: List<ParsedWorkoutRow>, val warnings: List<String>)

/** FR-153. Ergebnis eines kompletten Imports (Parsen + Uebungsabgleich + Speichern). */
data class ImportSummary(
    val sessionsImported: Int,
    val setsImported: Int,
    val unmatchedExercises: Map<String, Int>,
    val warnings: List<String>,
)
