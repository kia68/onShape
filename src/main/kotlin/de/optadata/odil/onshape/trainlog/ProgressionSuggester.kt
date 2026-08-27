package de.optadata.odil.onshape.trainlog

data class LastSetValues(val weightKg: Double?, val reps: Int?, val rir: Int?)

data class PrefillSuggestion(
    val lastWeightKg: Double?,
    val lastReps: Int?,
    val lastRir: Int?,
    val suggestedWeightKg: Double?,
    val suggestedReps: Int?,
    /** FR-94, vom Aufrufer per [WarmupSetCalculator] nachtraeglich befuellt (siehe
     * [de.optadata.odil.onshape.trainlog.WorkoutLogService.prefill]) -- bewusst NICHT hier in
     * [suggest] berechnet, damit diese Klasse bei der reinen Gewichts-/Wiederholungs-Progression
     * bleibt. */
    val warmupSets: List<WarmupSet> = emptyList(),
)

/**
 * FR-90/91: "vorbelegte Werte aus der letzten Einheit + Progressionsvorschlag". KONZEPT.md
 * beschreibt keine Formel -- Interpretation ist die klassische "doppelte Progression": solange
 * die obere Wiederholungsgrenze (`repMax`) noch nicht bei ausreichend niedrigem RIR erreicht ist,
 * wird zuerst eine Wiederholung mehr vorgeschlagen (gleiches Gewicht); erst wenn `repMax` bei
 * Ziel-RIR (oder ohne RIR-Angabe) erreicht ist, steigt das Gewicht (fester Sprung, siehe
 * [WEIGHT_INCREMENT_KG] -- ohne Equipment-Granularitaet pro Satz ist ein fixer Kleinschritt die
 * einfachste sichere Annahme, lieber zu klein als zu grob).
 */
object ProgressionSuggester {
    private const val WEIGHT_INCREMENT_KG = 2.5

    fun suggest(last: LastSetValues?, repMax: Int?, targetRir: Int?): PrefillSuggestion {
        if (last?.weightKg == null || last.reps == null) {
            return PrefillSuggestion(last?.weightKg, last?.reps, last?.rir, null, null)
        }
        val hitRepCap = repMax != null && last.reps >= repMax
        val hitRirTarget = targetRir == null || last.rir == null || last.rir <= targetRir

        val (suggestedWeight, suggestedReps) = if (hitRepCap && hitRirTarget) {
            (last.weightKg + WEIGHT_INCREMENT_KG) to repMax
        } else if (repMax != null && last.reps < repMax) {
            last.weightKg to (last.reps + 1).coerceAtMost(repMax)
        } else {
            last.weightKg to last.reps
        }
        return PrefillSuggestion(last.weightKg, last.reps, last.rir, suggestedWeight, suggestedReps)
    }
}
