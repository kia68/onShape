package de.optadata.odil.onshape.trainlog

enum class PersonalRecordType { MAX_WEIGHT, MAX_REPS_AT_WEIGHT, EST_1RM, VOLUME }

data class PersonalRecord(val type: PersonalRecordType, val previousBest: Double?, val newValue: Double)

/** Bisherige Bestwerte fuer eine Uebung, VOR dem gerade geloggten Satz. */
data class PriorBests(
    val maxWeightKg: Double? = null,
    /** Meiste Wiederholungen bei mindestens dem Gewicht des neuen Satzes (siehe [PersonalRecordDetector]-KDoc). */
    val maxRepsAtOrAboveWeight: Int? = null,
    val maxEstimated1Rm: Double? = null,
    val maxSetVolume: Double? = null,
)

data class LoggedSet(val weightKg: Double?, val reps: Int?, val isWarmup: Boolean, val completed: Boolean)

/** Ein abgeschlossener, nicht-Aufwaermsatz aus der Historie (fuer [PersonalRecordDetector.priorBestsFrom]
 * und die 1RM-Verlaufskurve, FR-97). */
data class WorkingSetSample(val weightKg: Double, val reps: Int)

/**
 * FR-98: "Gewicht, Wiederholungen, geschaetztes 1RM, Volumen" -- KONZEPT.md nennt nur die vier
 * Dimensionen, nicht deren genaue Definition. Interpretationsentscheidungen:
 * - Wiederholungen-PR ist NICHT die absolute Wiederholungszahl (die waere mit einem leichten
 *   Aufwaermsatz trivial zu knacken), sondern "mehr Wiederholungen als je zuvor bei MINDESTENS
 *   diesem Gewicht" -- die ueblich Definition in Trainings-Apps (z.B. Strong).
 * - Volumen-PR ist pro EINZELSATZ (Gewicht x Wiederholungen), nicht Session-Summe -- gleiche
 *   Granularitaet wie die anderen drei Dimensionen, ohne eine zusaetzliche Session-Aggregation
 *   zu brauchen.
 * Aufwaermsaetze und nicht abgeschlossene Saetze zaehlen nie als PR.
 */
object PersonalRecordDetector {

    /** Aggregiert die bisherige Satzhistorie zu [PriorBests] fuer den neuen Satz mit [newWeightKg].
     * Reine Funktion, damit dieselbe Formel wie [detect] fuer das geschaetzte 1RM verwendet wird
     * (keine doppelte Definition in SQL). */
    fun priorBestsFrom(samples: List<WorkingSetSample>, newWeightKg: Double): PriorBests {
        if (samples.isEmpty()) return PriorBests()
        return PriorBests(
            maxWeightKg = samples.maxOf { it.weightKg },
            maxRepsAtOrAboveWeight = samples.filter { it.weightKg >= newWeightKg }.maxOfOrNull { it.reps },
            maxEstimated1Rm = samples.mapNotNull { OneRepMaxCalculator.estimate(it.weightKg, it.reps) }.maxOrNull(),
            maxSetVolume = samples.maxOf { it.weightKg * it.reps },
        )
    }

    fun detect(set: LoggedSet, prior: PriorBests): List<PersonalRecord> {
        if (set.isWarmup || !set.completed || set.weightKg == null || set.reps == null || set.weightKg <= 0 || set.reps <= 0) {
            return emptyList()
        }
        val results = mutableListOf<PersonalRecord>()
        if (prior.maxWeightKg == null || set.weightKg > prior.maxWeightKg) {
            results += PersonalRecord(PersonalRecordType.MAX_WEIGHT, prior.maxWeightKg, set.weightKg)
        }
        if (prior.maxRepsAtOrAboveWeight == null || set.reps > prior.maxRepsAtOrAboveWeight) {
            results += PersonalRecord(PersonalRecordType.MAX_REPS_AT_WEIGHT, prior.maxRepsAtOrAboveWeight?.toDouble(), set.reps.toDouble())
        }
        OneRepMaxCalculator.estimate(set.weightKg, set.reps)?.let { est1Rm ->
            if (prior.maxEstimated1Rm == null || est1Rm > prior.maxEstimated1Rm) {
                results += PersonalRecord(PersonalRecordType.EST_1RM, prior.maxEstimated1Rm, est1Rm)
            }
        }
        val volume = set.weightKg * set.reps
        if (prior.maxSetVolume == null || volume > prior.maxSetVolume) {
            results += PersonalRecord(PersonalRecordType.VOLUME, prior.maxSetVolume, volume)
        }
        return results
    }
}
