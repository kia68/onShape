package de.optadata.odil.onshape.training

import java.util.UUID
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class ExerciseScorerTest {

    private fun exercise(
        slug: String,
        pattern: MovementPattern = MovementPattern.SQUAT,
        mechanic: Mechanic = Mechanic.COMPOUND,
        equipment: List<String> = listOf("bodyweight"),
        difficulty: ExerciseDifficulty = ExerciseDifficulty.BEGINNER,
        contraindications: List<String> = emptyList(),
        muscles: List<ExerciseMuscleFactor> = listOf(ExerciseMuscleFactor("quads", 1.0)),
    ) = Exercise(UUID.randomUUID(), slug, slug, pattern, mechanic, equipment, difficulty, false, null, contraindications, muscles)

    private fun context(
        equipment: Set<String> = setOf("bodyweight"),
        injuries: Set<String> = emptySet(),
        maxDifficulty: ExerciseDifficulty = ExerciseDifficulty.ADVANCED,
        alreadySelectedMuscles: Set<String> = emptySet(),
        alreadySelectedExerciseIds: Set<UUID> = emptySet(),
        rejectionCounts: Map<UUID, Int> = emptyMap(),
    ) = ScoringContext(equipment, injuries, maxDifficulty, alreadySelectedMuscles, alreadySelectedExerciseIds, rejectionCounts)

    @Test
    fun `kandidaten werden auf das gesuchte bewegungsmuster gefiltert`() {
        val squat = exercise("squat", pattern = MovementPattern.SQUAT)
        val bench = exercise("bench", pattern = MovementPattern.PUSH_HORIZONTAL)
        val candidates = ExerciseScorer.candidatesFor(MovementPattern.SQUAT, listOf(squat, bench), context())
        assertEquals(listOf(squat), candidates)
    }

    @Test
    fun `fehlendes equipment schliesst eine uebung hart aus`() {
        val barbellSquat = exercise("back-squat", equipment = listOf("barbell"))
        val candidates = ExerciseScorer.candidatesFor(MovementPattern.SQUAT, listOf(barbellSquat), context(equipment = setOf("bodyweight")))
        assertTrue(candidates.isEmpty())
    }

    @Test
    fun `kontraindikation schliesst eine uebung hart aus (verletzungssicherheit)`() {
        val kneeUnsafe = exercise("back-squat", contraindications = listOf("knee"))
        val candidates = ExerciseScorer.candidatesFor(MovementPattern.SQUAT, listOf(kneeUnsafe), context(injuries = setOf("knee")))
        assertTrue(candidates.isEmpty())
    }

    @Test
    fun `zu hohe schwierigkeit schliesst eine uebung fuer anfaenger aus`() {
        val advanced = exercise("front-squat", difficulty = ExerciseDifficulty.ADVANCED)
        val candidates = ExerciseScorer.candidatesFor(MovementPattern.SQUAT, listOf(advanced), context(maxDifficulty = ExerciseDifficulty.BEGINNER))
        assertTrue(candidates.isEmpty())
    }

    @Test
    fun `verbundsuebungen werden gegenueber isolationsuebungen bevorzugt (zeitbudget)`() {
        val compound = exercise("compound", mechanic = Mechanic.COMPOUND)
        val isolation = exercise("isolation", mechanic = Mechanic.ISOLATION)
        assertTrue(ExerciseScorer.score(compound, context()) > ExerciseScorer.score(isolation, context()))
    }

    @Test
    fun `redundanz zum zielmuskel senkt den score`() {
        val ex = exercise("ex", muscles = listOf(ExerciseMuscleFactor("chest", 1.0)))
        val withoutRedundancy = ExerciseScorer.score(ex, context())
        val withRedundancy = ExerciseScorer.score(ex, context(alreadySelectedMuscles = setOf("chest")))
        assertTrue(withRedundancy < withoutRedundancy)
    }

    @Test
    fun `abgelehnte uebungen werden niedriger bewertet (FR-74 nutzermodell)`() {
        val ex = exercise("ex")
        val neverRejected = ExerciseScorer.score(ex, context())
        val rejectedTwice = ExerciseScorer.score(ex, context(rejectionCounts = mapOf(ex.id to 2)))
        assertTrue(rejectedTwice < neverRejected)
    }

    @Test
    fun `pickBest liefert null wenn kein kandidat passt`() {
        val result = ExerciseScorer.pickBest(MovementPattern.SQUAT, emptyList(), context())
        assertNull(result)
    }

    @Test
    fun `pickBest bevorzugt den hoechsten score`() {
        val weak = exercise("weak", mechanic = Mechanic.ISOLATION)
        val strong = exercise("strong", mechanic = Mechanic.COMPOUND)
        val result = ExerciseScorer.pickBest(MovementPattern.SQUAT, listOf(weak, strong), context())
        assertEquals(strong, result)
    }

    @Test
    fun `findAlternative schliesst die abgelehnte uebung selbst aus`() {
        val rejected = exercise("rejected")
        val alternative = exercise("alternative")
        val result = ExerciseScorer.findAlternative(rejected, listOf(rejected, alternative), context())
        assertEquals(alternative, result)
    }
}
