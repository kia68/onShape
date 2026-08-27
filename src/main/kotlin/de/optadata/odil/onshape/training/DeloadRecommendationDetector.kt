package de.optadata.odil.onshape.training

enum class DeloadReason { STAGNANT_PERFORMANCE, MISSED_RIR_TARGET, HIGH_EXHAUSTION, PROLONGED_CALORIE_DEFICIT }

data class DeloadInput(
    /** Anzahl regelmaessig trainierter Uebungen mit >= 3 Wochen Historie, deren geschaetztes 1RM
     * in der juengsten Woche NICHT hoeher liegt als vor 3 Wochen (siehe Detector-KDoc). */
    val stagnantExerciseCount: Int,
    /** Saetze mit sowohl geloggtem als auch programmiertem Ziel-RIR, bei denen das tatsaechliche
     * RIR das Ziel klar verfehlt hat (siehe Detector-KDoc fuer die Richtung). */
    val rirMisses: Int,
    /** `perceivedEffort` (1-10) der zuletzt beendeten Sessions, NEUESTE ZUERST. */
    val recentPerceivedEfforts: List<Int>,
    /** Aufeinanderfolgende, bis heute reichende Wochen mit Kalorien-Logging-Durchschnitt unter
     * dem Tagesziel (siehe Detector-KDoc). */
    val weeksInCalorieDeficit: Int,
)

/**
 * FR-79 (KONZEPT.md §7.4, Issue #74): "Bei 3 Wochen stagnierender Leistung, wiederholt
 * verfehltem RIR-Ziel, subjektiv hoher Erschoepfung, oder >8 Wochen im Kaloriendefizit." --
 * eine reine EMPFEHLUNG (kein automatisches Eingreifen ins Programm, das jeder generierte
 * Mesozyklus ohnehin schon selbst mit einer geplanten Deload-Woche abschliesst, siehe
 * [MesocycleProgression]). Reine, DB-freie Kernlogik (NFR-13 testbar), jede Schwelle unten
 * explizit dokumentierte Interpretationsentscheidung -- KONZEPT nennt selbst keine Zahlen ausser
 * "3 Wochen" und "8 Wochen".
 *
 * "Wiederholt verfehltes RIR-Ziel": Interpretation = das TATSAECHLICHE RIR bleibt wiederholt
 * HOEHER als das programmierte Ziel (der Satz faellt leichter aus als geplant -- die Person
 * kann die vorgesehene Intensitaet nicht mehr erreichen). Die Gegenrichtung (haerter als geplant)
 * waere kein Erschoepfungssignal, sondern eher ein Zeichen von zu konservativer Programmierung --
 * passt nicht zum Cluster der anderen drei (allesamt Erschoepfungs-/Stagnationssignale).
 */
object DeloadRecommendationDetector {

    const val MIN_STAGNANT_EXERCISES = 1
    const val MIN_RIR_MISSES = 3
    const val HIGH_EXHAUSTION_THRESHOLD = 9
    const val HIGH_EXHAUSTION_MIN_SESSIONS = 3
    const val MAX_WEEKS_IN_DEFICIT = 8

    fun evaluate(input: DeloadInput): Set<DeloadReason> {
        val reasons = mutableSetOf<DeloadReason>()

        if (input.stagnantExerciseCount >= MIN_STAGNANT_EXERCISES) reasons += DeloadReason.STAGNANT_PERFORMANCE
        if (input.rirMisses >= MIN_RIR_MISSES) reasons += DeloadReason.MISSED_RIR_TARGET

        val recent = input.recentPerceivedEfforts.take(HIGH_EXHAUSTION_MIN_SESSIONS)
        if (recent.size >= HIGH_EXHAUSTION_MIN_SESSIONS && recent.average() >= HIGH_EXHAUSTION_THRESHOLD) {
            reasons += DeloadReason.HIGH_EXHAUSTION
        }

        if (input.weeksInCalorieDeficit > MAX_WEEKS_IN_DEFICIT) reasons += DeloadReason.PROLONGED_CALORIE_DEFICIT

        return reasons
    }
}
