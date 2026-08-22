package de.optadata.odil.onshape.training

import de.optadata.odil.onshape.onboarding.Experience

data class VolumeCorridorResult(val startSetsPerMuscle: Int, val maxSetsPerMuscle: Int, val weeklyIncrement: Double)

/** §7.4 Schritt 1, Tabelle "Unsere Volumenkorridore". Bereiche auf einen Punktwert reduziert
 * (Start = Untergrenze, Steigerung = Range-Mittelwert), analog zu den anderen
 * Prosa-zu-Formel-Umsetzungen in diesem Projekt. */
object VolumeCorridor {
    private val BEGINNER = VolumeCorridorResult(startSetsPerMuscle = 8, maxSetsPerMuscle = 10, weeklyIncrement = 0.5)
    private val INTERMEDIATE = VolumeCorridorResult(startSetsPerMuscle = 12, maxSetsPerMuscle = 16, weeklyIncrement = 1.0)
    private val ADVANCED = VolumeCorridorResult(startSetsPerMuscle = 14, maxSetsPerMuscle = 20, weeklyIncrement = 1.5)
    private const val OVER_60_MIN = 10
    private const val OVER_60_MAX = 14
    const val OVER_60_AGE_THRESHOLD = 60

    fun forProfile(experience: Experience, age: Int): VolumeCorridorResult {
        val base = when (experience) {
            Experience.NONE, Experience.BEGINNER -> BEGINNER
            Experience.INTERMEDIATE -> INTERMEDIATE
            Experience.ADVANCED -> ADVANCED
        }
        if (age <= OVER_60_AGE_THRESHOLD) return base
        return VolumeCorridorResult(OVER_60_MIN, OVER_60_MAX, base.weeklyIncrement)
    }

    /** "Ueber 60 ... dafuer 3x Frequenz" -- statt eine eigene Frequenz-Zahl zu erfinden, wird
     * das ueber denselben Mechanismus wie Anfaenger geloest: immer Ganzkoerper (hohe Frequenz
     * pro Muskelgruppe ist bei Ganzkoerper-Splits automatisch gegeben), siehe [ProgramGenerator]. */
    fun preferHighFrequencySplit(age: Int): Boolean = age > OVER_60_AGE_THRESHOLD
}
