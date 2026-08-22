package de.optadata.odil.onshape.training

import java.util.UUID

data class ScoringContext(
    val equipment: Set<String>,
    val injuries: Set<String>,
    val maxDifficulty: ExerciseDifficulty,
    val alreadySelectedMuscles: Set<String>,
    val alreadySelectedExerciseIds: Set<UUID>,
    val rejectionCounts: Map<UUID, Int>,
)

/**
 * §7.4 Schritt 3, Bewertungsfunktion. Ziel-Muskel-Match/Equipment-Verfuegbarkeit/
 * Verletzungs-Sicherheit sind in KONZEPT.md teils als "hartes Ausschlusskriterium" markiert --
 * die werden hier als FILTER umgesetzt (nicht als Score-Abzug), der Rest als gewichtete Summe.
 * Erfahrungs-Eignung ist ebenfalls ein Filter (Anfaenger duerfen keine advanced-Uebung ziehen),
 * keine weiche Praeferenz -- das deckt sich mit der harten Anfaenger-Regel in §7.4 Schritt 3
 * ("erst Maschine, dann Kurzhantel, dann Langhantel").
 */
object ExerciseScorer {
    private const val COMPOUND_BONUS = 2.0
    private const val ISOLATION_BONUS = 1.0
    private const val REDUNDANCY_PENALTY = 0.5
    private const val REJECTION_PENALTY = 0.75

    fun candidatesFor(pattern: MovementPattern, pool: List<Exercise>, context: ScoringContext): List<Exercise> =
        pool.filter { it.pattern == pattern }
            .filter { it.id !in context.alreadySelectedExerciseIds }
            .filter { it.equipment.any { eq -> eq in context.equipment } }
            .filter { it.contraindications.none { c -> c in context.injuries } }
            .filter { it.difficulty.atMost(context.maxDifficulty) }

    /** w6 Zeitbudget-Effizienz (Verbundsuebungen trainieren mehr in weniger Zeit),
     * w7 Redundanz (Abzug, wenn der Zielmuskel schon durch eine andere Uebung abgedeckt ist),
     * w8 Ablehnungshistorie (FR-74, Abzug je vergangenem Tausch). */
    fun score(exercise: Exercise, context: ScoringContext): Double {
        val timeBudgetScore = if (exercise.mechanic == Mechanic.COMPOUND) COMPOUND_BONUS else ISOLATION_BONUS
        val redundancy = exercise.primaryMuscles.count { it in context.alreadySelectedMuscles }
        val rejections = context.rejectionCounts[exercise.id] ?: 0
        return timeBudgetScore - redundancy * REDUNDANCY_PENALTY - rejections * REJECTION_PENALTY
    }

    fun pickBest(pattern: MovementPattern, pool: List<Exercise>, context: ScoringContext): Exercise? =
        candidatesFor(pattern, pool, context).maxByOrNull { score(it, context) }

    /** FR-74: Alternative mit gleichem Zielmuskel (selbes Bewegungsmuster) und verfuegbarem
     * Equipment, ohne die abgelehnte Uebung selbst. */
    fun findAlternative(rejected: Exercise, pool: List<Exercise>, context: ScoringContext): Exercise? =
        candidatesFor(rejected.pattern, pool, context.copy(alreadySelectedExerciseIds = context.alreadySelectedExerciseIds + rejected.id))
            .maxByOrNull { score(it, context) }
}
