package de.optadata.odil.onshape.training

import kotlin.math.roundToInt

data class WeekStage(val weekNumber: Int, val isDeload: Boolean, val targetRir: Int, val setsPerMuscle: Int)

/**
 * §7.4 Schritt 4 "Mesozyklus-Volumenprogression": fuer genau 5 Aufbauwochen + 1 Deload-Woche
 * konkret angegeben (RIR 3,2,2,1,0-1, dann Deload 50% Volumen/RIR 4). `programs.weeks` ist aber
 * konfigurierbar (Default 6, siehe V5__training.sql) -- die RIR-Kurve wird deshalb linear von 3
 * (Woche 1) auf 0 (letzte Aufbauwoche) generalisiert. Fuer den Default-Fall (6 Wochen = 5
 * Aufbau + 1 Deload) ergibt diese Formel exakt die in der Tabelle genannten Werte (siehe Tests).
 */
object MesocycleProgression {
    private const val START_RIR = 3
    private const val DELOAD_RIR = 4
    private const val DELOAD_VOLUME_FRACTION = 0.5

    fun stagesFor(corridor: VolumeCorridorResult, totalWeeks: Int): List<WeekStage> {
        require(totalWeeks >= 2) { "Ein Mesozyklus braucht mindestens eine Aufbauwoche + eine Deload-Woche" }
        val buildWeeks = totalWeeks - 1
        val buildStages = (1..buildWeeks).map { week ->
            val sets = (corridor.startSetsPerMuscle + (week - 1) * corridor.weeklyIncrement)
                .roundToInt()
                .coerceAtMost(corridor.maxSetsPerMuscle)
            WeekStage(week, isDeload = false, targetRir = rirForBuildWeek(week, buildWeeks), setsPerMuscle = sets)
        }
        val deloadSets = (buildStages.last().setsPerMuscle * DELOAD_VOLUME_FRACTION).roundToInt().coerceAtLeast(1)
        return buildStages + WeekStage(totalWeeks, isDeload = true, targetRir = DELOAD_RIR, setsPerMuscle = deloadSets)
    }

    private fun rirForBuildWeek(week: Int, buildWeeks: Int): Int {
        if (buildWeeks <= 1) return START_RIR - 1
        val fraction = (week - 1).toDouble() / (buildWeeks - 1)
        return (START_RIR - fraction * START_RIR).roundToInt().coerceIn(0, START_RIR)
    }
}
