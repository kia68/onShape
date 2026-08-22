package de.optadata.odil.onshape.integrations

import de.optadata.odil.onshape.training.ExerciseCatalogEntry
import java.util.UUID

/** FR-153: gleicht freie Uebungsnamen aus importierten Dateien (z.B. `"Snatch (Barbell)"`,
 * `"Bench Press (Barbell)"`) gegen den ~38 Eintraege umfassenden App-Uebungskatalog (V11-Seed) ab.
 * Bewusst KEIN Fuzzy-/NLP-Matching -- nur (1) normalisierter Wortmengen-Vergleich gegen `name_en`/
 * `name_de` und (2) eine kuratierte Alias-Tabelle fuer die haeufigsten Kraftsport-Grundbewegungen,
 * die anders benannt/geordnet sind als im App-Katalog (z.B. Hevy `"Squat (Barbell)"` vs. App
 * `"Back Squat"`). Alles andere bleibt bewusst unmatched und wird [ImportService] transparent an
 * den Nutzer zurueckgemeldet statt geraten. */
object ExerciseMatcher {

    /** Alias-Eintraege in natuerlicher Schreibweise -> Katalog-`slug`. Werden erst bei Zugriff auf
     * [ALIASES] normalisiert -- Schluessel HIER von Hand alphabetisch vorzusortieren waere
     * fehleranfaellig (siehe [normalize]: sortiert Woerter), also bewusst nicht gemacht. */
    private val RAW_ALIASES: Map<String, String> = mapOf(
        "barbell squat" to "back-squat",
        "squat" to "bodyweight-squat",
        "barbell bench press" to "barbell-bench-press",
        "bench press" to "dumbbell-bench-press",
        "barbell deadlift" to "barbell-deadlift",
        "deadlift" to "barbell-deadlift",
        "barbell overhead press" to "barbell-ohp",
        "barbell shoulder press" to "barbell-ohp",
        "overhead press" to "barbell-ohp",
        "shoulder press" to "dumbbell-shoulder-press",
        "assisted pull up" to "assisted-pullup",
        "pull up" to "pullup",
        "push up" to "pushup",
        "bicep curl" to "dumbbell-curl",
        "biceps curl" to "dumbbell-curl",
        "curl dumbbell" to "dumbbell-curl",
        "triceps pushdown" to "triceps-pushdown",
        "tricep pushdown" to "triceps-pushdown",
        "dumbbell lateral raise" to "lateral-raise",
        "lateral raise" to "lateral-raise",
        "calf raise" to "calf-raise",
        "dumbbell romanian deadlift" to "dumbbell-rdl",
        "romanian deadlift" to "dumbbell-rdl",
        "barbell hip thrust" to "hip-thrust",
        "hip thrust" to "hip-thrust",
        "kettlebell swing" to "kettlebell-swing",
        "farmers carry" to "farmers-carry",
        "lat pulldown" to "lat-pulldown",
        "seated cable row" to "seated-cable-row",
        "barbell row" to "barbell-row",
        "bent over row" to "barbell-row",
        "dumbbell row" to "dumbbell-row",
        "goblet squat" to "goblet-squat",
        "leg press" to "leg-press",
        "glute bridge" to "glute-bridge",
        "hanging leg raise" to "hanging-leg-raise",
        "dead bug" to "dead-bug",
        "plank" to "plank",
        "jumping jacks" to "jumping-jacks",
        "stationary bike" to "stationary-bike",
        "rowing machine" to "rowing-machine",
    )

    private val ALIASES: Map<String, String> by lazy { RAW_ALIASES.mapKeys { normalize(it.key) } }

    fun match(catalog: List<ExerciseCatalogEntry>, rawName: String): UUID? {
        val normalized = normalize(rawName)
        if (normalized.isBlank()) return null

        catalog.firstOrNull { normalize(it.nameEn) == normalized || normalize(it.nameDe) == normalized }?.let { return it.id }

        ALIASES[normalized]?.let { slug -> catalog.firstOrNull { it.slug == slug }?.let { return it.id } }

        return null
    }

    /** Kleinschreibung, Satzzeichen/Klammern zu Leerzeichen, Woerter alphabetisch sortiert und
     * mit einfachem Leerzeichen verbunden -- macht den Vergleich unabhaengig von Wortreihenfolge
     * und Klammer-Zusatzangaben ("Bench Press (Barbell)" vs. "Barbell Bench Press"). */
    private fun normalize(s: String): String =
        s.lowercase()
            .replace(Regex("[^a-z0-9]+"), " ")
            .trim()
            .split(" ")
            .filter { it.isNotBlank() }
            .sorted()
            .joinToString(" ")
}
