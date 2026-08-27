package de.optadata.odil.onshape.trainlog

import kotlin.math.roundToInt

data class WarmupSet(val weightKg: Double, val reps: Int)

/**
 * FR-94: "aus dem Arbeitsgewicht automatisch 2-3 Aufwaermsaetze". KONZEPT.md nennt nur die
 * Satzanzahl-Spanne, keine Formel -- alle Werte unten sind explizit dokumentierte
 * Interpretationsentscheidungen (Prozent-Rampe ist gaengige Trainingspraxis, keine
 * wissenschaftliche Formel wie bei den Energiebedarfs-Berechnungen in §7.1).
 *
 * - Unter [MIN_WORK_WEIGHT_KG] (leere Olympia-Langhantel, gleicher Referenzwert wie FR-110 in
 *   Epic #8 "leere Stange") wird KEIN Aufwaermsatz vorgeschlagen -- ein Aufwaermsatz unter dem
 *   Arbeitsgewicht waere dann schon leichter als die leere Stange selbst, also gegenstandslos.
 * - Ab [HEAVY_THRESHOLD_KG] werden 3 Saetze statt 2 vorgeschlagen (mehr Rampen-Stufen fuer
 *   schwerere Lasten, gaengige Praxis bei Langhantel-Grundbewegungen).
 * - Prozent-Rampe steigt, Wiederholungen sinken je Stufe (klassisches Warm-up-Muster: leicht +
 *   viele Wiederholungen zuerst, dann naeher ans Arbeitsgewicht mit wenigen Wiederholungen).
 * - Gerundet auf [WEIGHT_ROUNDING_KG] -- gleicher fixer Kleinschritt wie [ProgressionSuggester]
 *   ("ohne Equipment-Granularitaet pro Satz ist ein fixer Kleinschritt die einfachste sichere
 *   Annahme").
 */
object WarmupSetCalculator {

    const val MIN_WORK_WEIGHT_KG = 20.0
    const val HEAVY_THRESHOLD_KG = 60.0
    const val WEIGHT_ROUNDING_KG = 2.5

    private val TWO_SET_RAMP = listOf(0.5 to 5, 0.75 to 3)
    private val THREE_SET_RAMP = listOf(0.4 to 8, 0.6 to 5, 0.8 to 3)

    fun calculate(workWeightKg: Double?): List<WarmupSet> {
        if (workWeightKg == null || workWeightKg < MIN_WORK_WEIGHT_KG) return emptyList()
        val ramp = if (workWeightKg >= HEAVY_THRESHOLD_KG) THREE_SET_RAMP else TWO_SET_RAMP
        return ramp.map { (fraction, reps) ->
            val rounded = ((workWeightKg * fraction) / WEIGHT_ROUNDING_KG).roundToInt() * WEIGHT_ROUNDING_KG
            WarmupSet(rounded.coerceAtLeast(WEIGHT_ROUNDING_KG), reps)
        }
    }
}
