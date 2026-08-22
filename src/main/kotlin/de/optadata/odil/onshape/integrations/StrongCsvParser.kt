package de.optadata.odil.onshape.integrations

import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter

/** Parst den Strong-App-Export ("Profil -> Settings -> Export Data"): Header exakt
 * `Date,Workout Name,Duration,Exercise Name,Set Order,Weight,Reps,Distance,Seconds,Notes,
 * Workout Notes,RPE`, eine Zeile pro Satz. Verifiziert gegen eine echte exportierte Beispieldatei
 * (nicht nur Doku), siehe docs/progress.md.
 *
 * Zwei bewusste Vereinfachungen, weil die Datei die noetige Information nicht eindeutig liefert
 * (siehe FR-153-Abschnitt in docs/progress.md):
 * - `Weight`: die Einheit (kg/lbs) ist eine App-Einstellung des Nutzers und steht NICHT in der
 *   Datei -- der Rohwert wird unveraendert als kg uebernommen und per Warnung kenntlich gemacht,
 *   statt eine Einheit zu erraten.
 * - `Distance`: gleiches Einheiten-Problem (km/miles), aber hier waere ein falsch interpretierter
 *   Rohwert um den vollen Umrechnungsfaktor falsch (anders als bei Gewicht, wo kg/lbs nur um
 *   Faktor ~2.2 auseinanderliegen) -- Distanzwerte werden deshalb NICHT importiert, nur gezaehlt
 *   und als Warnung gemeldet. */
object StrongCsvParser {

    private val DATE_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
    private val DURATION_PATTERN = Regex("""(?:(\d+)h)?\s*(?:(\d+)m)?\s*(?:(\d+)s)?""")

    fun parse(csvText: String): ParseResult {
        val records = CsvReader.parseWithHeader(csvText)
        val rows = mutableListOf<ParsedWorkoutRow>()
        var skippedNoExercise = 0
        var weightRowCount = 0
        var droppedDistanceCount = 0

        for (rec in records) {
            val exerciseName = rec["Exercise Name"]?.trim().orEmpty()
            if (exerciseName.isBlank()) {
                skippedNoExercise++
                continue
            }
            val startedAt = parseDate(rec["Date"]) ?: continue
            val workoutName = rec["Workout Name"]?.trim().orEmpty()
            val duration = parseHumanDuration(rec["Duration"])
            val finishedAt = duration?.let { startedAt.plusSeconds(it) }

            val weightKg = rec["Weight"]?.toDoubleOrNullSafe()
            if (weightKg != null) weightRowCount++
            val distanceRaw = rec["Distance"]?.toDoubleOrNullSafe()
            if (distanceRaw != null && distanceRaw > 0) droppedDistanceCount++

            val setOrder = rec["Set Order"]?.toIntOrNull() ?: 1

            rows.add(
                ParsedWorkoutRow(
                    sessionKey = "$startedAt|$workoutName",
                    sessionTitle = workoutName.ifBlank { "Strong-Workout" },
                    startedAt = startedAt,
                    finishedAt = finishedAt,
                    exerciseName = exerciseName,
                    setIndex = maxOf(0, setOrder - 1),
                    weightKg = weightKg,
                    reps = rec["Reps"]?.toIntOrNull(),
                    durationSec = rec["Seconds"]?.toIntOrNull()?.takeIf { it > 0 },
                    distanceM = null,
                ),
            )
        }

        val warnings = mutableListOf<String>()
        if (skippedNoExercise > 0) warnings.add("$skippedNoExercise Zeile(n) ohne Uebungsnamen (Exercise Name) uebersprungen.")
        if (weightRowCount > 0) {
            warnings.add(
                "Gewichtswerte wurden unveraendert aus der CSV uebernommen und als kg gespeichert -- " +
                    "Strong speichert die Einheit (kg/lbs) als App-Einstellung, nicht in der Exportdatei. Bitte pruefen.",
            )
        }
        if (droppedDistanceCount > 0) {
            warnings.add(
                "$droppedDistanceCount Distanzwert(e) NICHT importiert -- die Einheit (km/miles) ist in der " +
                    "Strong-Exportdatei nicht eindeutig bestimmbar.",
            )
        }
        return ParseResult(rows, warnings)
    }

    private fun parseDate(raw: String?): Instant? {
        val trimmed = raw?.trim().orEmpty()
        if (trimmed.isBlank()) return null
        return runCatching { LocalDateTime.parse(trimmed, DATE_FORMAT).toInstant(ZoneOffset.UTC) }.getOrNull()
    }

    /** Parst Strings wie `"2h 38m"`, `"45m"`, `"1h"`, `"5m 30s"` in Gesamtsekunden. */
    private fun parseHumanDuration(raw: String?): Long? {
        val trimmed = raw?.trim().orEmpty()
        if (trimmed.isBlank()) return null
        val match = DURATION_PATTERN.matchEntire(trimmed) ?: return null
        val (h, m, s) = match.destructured
        if (h.isBlank() && m.isBlank() && s.isBlank()) return null
        return (h.toLongOrNull() ?: 0) * 3600 + (m.toLongOrNull() ?: 0) * 60 + (s.toLongOrNull() ?: 0)
    }

    private fun String.toDoubleOrNullSafe(): Double? = trim().takeIf { it.isNotBlank() }?.toDoubleOrNull()
}
