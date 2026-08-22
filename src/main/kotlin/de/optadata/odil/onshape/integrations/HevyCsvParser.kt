package de.optadata.odil.onshape.integrations

import java.time.LocalDateTime
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.util.Locale

/** Parst den offiziellen Hevy-Export ("Profil -> Export & Import Data -> Export Workouts"):
 * Spalten `title,start_time,end_time,description,exercise_title,superset_id,exercise_notes,
 * set_index,set_type,weight_kg|weight_lbs,reps,distance_km|distance_miles,duration_seconds,rpe`,
 * eine Zeile pro Satz. Format verifiziert gegen eine echte Beispieldatei (nicht nur Doku), siehe
 * docs/progress.md Eintrag zu Epic Integrationen. Bewusst reine Funktion (kein DB-/HTTP-Zugriff)
 * fuer isolierte Tests, analog zu [de.optadata.odil.onshape.progress.CsvWriter]. */
object HevyCsvParser {

    private val DATE_FORMAT = DateTimeFormatter.ofPattern("d MMM yyyy, HH:mm", Locale.ENGLISH)
    private const val LBS_TO_KG = 0.45359237
    private const val MILES_TO_M = 1609.344

    fun parse(csvText: String): ParseResult {
        val records = CsvReader.parseWithHeader(csvText)
        val rows = mutableListOf<ParsedWorkoutRow>()
        val warnings = mutableListOf<String>()
        var skippedNoExercise = 0

        for (rec in records) {
            val exerciseTitle = rec["exercise_title"]?.trim().orEmpty()
            if (exerciseTitle.isBlank()) {
                skippedNoExercise++
                continue
            }
            val title = rec["title"]?.trim().orEmpty()
            val startedAt = parseDate(rec["start_time"]) ?: continue
            val finishedAt = parseDate(rec["end_time"])

            val weightKg = rec["weight_kg"]?.toDoubleOrNullSafe()
                ?: rec["weight_lbs"]?.toDoubleOrNullSafe()?.let { it * LBS_TO_KG }
            val distanceM = rec["distance_km"]?.toDoubleOrNullSafe()?.let { it * 1000.0 }
                ?: rec["distance_miles"]?.toDoubleOrNullSafe()?.let { it * MILES_TO_M }
            val durationSec = rec["duration_seconds"]?.toDoubleOrNullSafe()?.toInt()
            val setIndex = rec["set_index"]?.toIntOrNull() ?: 0

            rows.add(
                ParsedWorkoutRow(
                    sessionKey = "$startedAt|$title",
                    sessionTitle = title.ifBlank { "Hevy-Workout" },
                    startedAt = startedAt,
                    finishedAt = finishedAt,
                    exerciseName = exerciseTitle,
                    setIndex = setIndex,
                    weightKg = weightKg,
                    reps = rec["reps"]?.toIntOrNull(),
                    durationSec = durationSec?.takeIf { it > 0 },
                    distanceM = distanceM?.takeIf { it > 0 },
                ),
            )
        }

        if (skippedNoExercise > 0) {
            warnings.add("$skippedNoExercise Zeile(n) ohne Uebungsnamen (exercise_title) uebersprungen.")
        }
        return ParseResult(rows, warnings)
    }

    private fun parseDate(raw: String?): java.time.Instant? {
        val trimmed = raw?.trim().orEmpty()
        if (trimmed.isBlank()) return null
        return runCatching { LocalDateTime.parse(trimmed, DATE_FORMAT).toInstant(ZoneOffset.UTC) }.getOrNull()
    }

    private fun String.toDoubleOrNullSafe(): Double? = trim().takeIf { it.isNotBlank() }?.toDoubleOrNull()
}
