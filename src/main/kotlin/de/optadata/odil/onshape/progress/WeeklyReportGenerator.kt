package de.optadata.odil.onshape.progress

/** Einstufung je Dimension -- bewusst NICHT "gut"/"schlecht" moralisch, sondern
 * "brauucht Aufmerksamkeit" vs. neutral vs. gut, konsistent mit der Wellbeing-Guardrail-Sprache
 * aus Epic #12 (KONZEPT.md §14.5: "wertungsfrei", "passt heute nicht so gut" statt "schlecht"). */
enum class Rating { GOOD, NEUTRAL, NEEDS_ATTENTION }

/** GENAU EINE Empfehlung (FR-135: "eine konkrete Empfehlung") -- Prioritaetsreihenfolge unten
 * in [WeeklyReportGenerator.generate] dokumentiert. */
enum class WeeklyReportRecommendation { FOCUS_ON_TRAINING_SESSIONS, FOCUS_ON_NUTRITION_LOGGING, NUTRITION_OFF_TARGET, ON_TRACK }

data class WeeklyReportInput(
    val sessionsCompleted: Int,
    val sessionsPlanned: Int,
    val nutritionDaysLogged: Int,
    /** null, wenn diese Woche kein einziger Tag geloggt wurde. */
    val avgKcal: Double?,
    /** null, wenn (noch) kein Tagesziel berechnet wurde (Onboarding unvollstaendig). */
    val targetKcal: Int?,
)

data class WeeklyReportResult(
    val trainingRating: Rating,
    val nutritionLoggingRating: Rating,
    /** null = nicht beurteilbar (kein Tag geloggt oder kein Zielwert vorhanden). */
    val nutritionTargetRating: Rating?,
    val recommendation: WeeklyReportRecommendation,
)

/**
 * FR-135 (KONZEPT.md §15.1, Plus/Coach-Feature): "was lief gut, was nicht, eine konkrete
 * Empfehlung" -- Prosa ohne Zahlen, jede Schwelle unten ist eine explizit dokumentierte
 * Interpretationsentscheidung (gleiches Muster wie [de.optadata.odil.onshape.wellbeing.WellbeingPatternDetector]).
 * Reine, DB-freie Kernlogik (NFR-13 testbar) -- liefert nur strukturierte Einstufungen, keinen
 * Fliesstext: die eigentlichen Saetze entstehen im Frontend per next-intl (NFR-11, gleiches
 * Muster wie [de.optadata.odil.onshape.wellbeing.WellbeingFlag]), damit KEINE Sprachentscheidung
 * (insbesondere die im Wellbeing-Kapitel geforderte wertungsfreie Tonalitaet) im Backend
 * hartkodiert ist.
 *
 * Koerpergewicht ist bewusst NICHT Teil der Bewertung: §14.5 verbietet explizit eine moralische
 * Bewertung von Gewichtsschwankungen ("keine roten Warnfarben") -- ein woechentlicher
 * Gewichtstrend darf informativ angezeigt werden, aber nicht in "gut"/"braucht Aufmerksamkeit"
 * einsortiert werden.
 */
object WeeklyReportGenerator {

    /** "Kein einziges geplantes Training absolviert" ist der konkreteste, am direktesten
     * umsetzbare Hebel (KONZEPT.md §16-Analyse: "Retention-Krise ist ein Kompetenz-, kein
     * Motivationsproblem") -- deshalb hoechste Prioritaet fuer die einzige Empfehlung. */
    private fun trainingRating(completed: Int, planned: Int): Rating = when {
        planned <= 0 -> Rating.NEUTRAL
        completed >= planned -> Rating.GOOD
        completed == 0 -> Rating.NEEDS_ATTENTION
        else -> Rating.NEUTRAL
    }

    /** Adhaerenz (Tage geloggt / 7) als Tracking-KONSISTENZ, nicht Zielkonformitaet -- gleiche
     * Definition wie [AdherenceCalculator]. Schwellen: "fast jeden Tag" (>=6/7) ist gut, "an
     * weniger als der Haelfte der Tage" (<3/7) braucht Aufmerksamkeit, dazwischen neutral. */
    private fun nutritionLoggingRating(daysLogged: Int): Rating = when {
        daysLogged >= 6 -> Rating.GOOD
        daysLogged < 3 -> Rating.NEEDS_ATTENTION
        else -> Rating.NEUTRAL
    }

    /** Abweichung vom Tagesziel symmetrisch in BEIDE Richtungen bewertet (KONZEPT.md §14.5:
     * "keine roten Warnfarben bei Zielueberschreitung" -- Ueberschreiten ist nicht schlimmer als
     * Unterschreiten). +-10% gilt als nah genug am Ziel, jenseits +-25% braucht es Aufmerksamkeit. */
    private fun nutritionTargetRating(avgKcal: Double?, targetKcal: Int?): Rating? {
        if (avgKcal == null || targetKcal == null || targetKcal <= 0) return null
        val deviation = kotlin.math.abs(avgKcal - targetKcal) / targetKcal
        return when {
            deviation <= 0.10 -> Rating.GOOD
            deviation > 0.25 -> Rating.NEEDS_ATTENTION
            else -> Rating.NEUTRAL
        }
    }

    fun generate(input: WeeklyReportInput): WeeklyReportResult {
        val training = trainingRating(input.sessionsCompleted, input.sessionsPlanned)
        val nutritionLogging = nutritionLoggingRating(input.nutritionDaysLogged)
        val nutritionTarget = nutritionTargetRating(input.avgKcal, input.targetKcal)

        // Prioritaet: Training vor Logging vor Makro-Praezision -- je konkreter/umsetzbarer der
        // Hebel, desto eher wird er als DIE eine Empfehlung gewaehlt (siehe Klassen-KDoc).
        val recommendation = when {
            training == Rating.NEEDS_ATTENTION -> WeeklyReportRecommendation.FOCUS_ON_TRAINING_SESSIONS
            nutritionLogging == Rating.NEEDS_ATTENTION -> WeeklyReportRecommendation.FOCUS_ON_NUTRITION_LOGGING
            nutritionTarget == Rating.NEEDS_ATTENTION -> WeeklyReportRecommendation.NUTRITION_OFF_TARGET
            else -> WeeklyReportRecommendation.ON_TRACK
        }

        return WeeklyReportResult(training, nutritionLogging, nutritionTarget, recommendation)
    }
}
