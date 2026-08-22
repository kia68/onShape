package de.optadata.odil.onshape.training

import de.optadata.odil.onshape.onboarding.Experience
import de.optadata.odil.onshape.onboarding.Goal
import java.util.UUID
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class ProgramGeneratorTest {

    private fun exercise(
        slug: String,
        pattern: MovementPattern,
        mechanic: Mechanic = Mechanic.COMPOUND,
        equipment: List<String>,
        difficulty: ExerciseDifficulty,
        contraindications: List<String> = emptyList(),
        muscle: String,
    ) = Exercise(UUID.randomUUID(), slug, slug, pattern, mechanic, equipment, difficulty, false, if (pattern == MovementPattern.CARDIO) 7.0 else null, contraindications, listOf(ExerciseMuscleFactor(muscle, 1.0)))

    /** Deckt jedes Bewegungsmuster mit einer Koerpergewichts- (beginner) und einer
     * Langhantel-Variante (advanced) ab, plus eine kniebelastende Variante fuer den
     * Verletzungssicherheits-Test. */
    private val pool = listOf(
        exercise("bw-squat", MovementPattern.SQUAT, equipment = listOf("bodyweight"), difficulty = ExerciseDifficulty.BEGINNER, muscle = "quads"),
        exercise("bb-squat", MovementPattern.SQUAT, equipment = listOf("barbell"), difficulty = ExerciseDifficulty.ADVANCED, contraindications = listOf("knee"), muscle = "quads"),
        exercise("bw-hinge", MovementPattern.HINGE, equipment = listOf("bodyweight"), difficulty = ExerciseDifficulty.BEGINNER, muscle = "hamstrings"),
        exercise("bb-hinge", MovementPattern.HINGE, equipment = listOf("barbell"), difficulty = ExerciseDifficulty.ADVANCED, muscle = "hamstrings"),
        exercise("bw-push-h", MovementPattern.PUSH_HORIZONTAL, equipment = listOf("bodyweight"), difficulty = ExerciseDifficulty.BEGINNER, muscle = "chest"),
        exercise("bb-push-h", MovementPattern.PUSH_HORIZONTAL, equipment = listOf("barbell"), difficulty = ExerciseDifficulty.ADVANCED, muscle = "chest"),
        exercise("bw-push-v", MovementPattern.PUSH_VERTICAL, equipment = listOf("bodyweight"), difficulty = ExerciseDifficulty.BEGINNER, muscle = "front_delt"),
        exercise("bb-push-v", MovementPattern.PUSH_VERTICAL, equipment = listOf("barbell"), difficulty = ExerciseDifficulty.ADVANCED, muscle = "front_delt"),
        exercise("bw-pull-h", MovementPattern.PULL_HORIZONTAL, equipment = listOf("bands"), difficulty = ExerciseDifficulty.BEGINNER, muscle = "upper_back"),
        exercise("bb-pull-h", MovementPattern.PULL_HORIZONTAL, equipment = listOf("barbell"), difficulty = ExerciseDifficulty.ADVANCED, muscle = "lats"),
        exercise("bw-pull-v", MovementPattern.PULL_VERTICAL, equipment = listOf("bands"), difficulty = ExerciseDifficulty.BEGINNER, muscle = "lats"),
        exercise("bb-pull-v", MovementPattern.PULL_VERTICAL, equipment = listOf("pullup_bar"), difficulty = ExerciseDifficulty.ADVANCED, muscle = "lats"),
        exercise("core-plank", MovementPattern.CORE, mechanic = Mechanic.ISOLATION, equipment = listOf("bodyweight"), difficulty = ExerciseDifficulty.BEGINNER, muscle = "abs"),
        exercise("cardio-bike", MovementPattern.CARDIO, equipment = listOf("gym"), difficulty = ExerciseDifficulty.BEGINNER, muscle = "quads"),
    )

    private fun input(
        goal: Goal = Goal.MAINTAIN,
        experience: Experience = Experience.INTERMEDIATE,
        age: Int = 30,
        equipment: List<String> = listOf("bodyweight", "barbell", "bands", "pullup_bar", "gym"),
        injuries: List<String> = emptyList(),
        trainingDaysWeek: Int = 4,
        sessionMinutes: Int = 60,
        weeks: Int = 6,
    ) = ProgramGeneratorInput(goal, experience, age, equipment, injuries, trainingDaysWeek, sessionMinutes, weeks)

    @Test
    fun `generiert fuer jede woche und jeden tag einen eintrag`() {
        val program = ProgramGenerator.generate(input(trainingDaysWeek = 4, weeks = 6), pool)
        assertEquals(6 * 4, program.days.size)
    }

    @Test
    fun `letzte woche ist die deload-woche mit weniger saetzen`() {
        val program = ProgramGenerator.generate(input(weeks = 6), pool)
        val week5Day0 = program.days.first { it.weekNumber == 5 && it.dayIndex == 0 }
        val week6Day0 = program.days.first { it.weekNumber == 6 && it.dayIndex == 0 }
        assertTrue(week6Day0.isDeload)
        assertFalse(week5Day0.isDeload)
        val week5Sets = week5Day0.items.filter { it.durationMinutes == null }.sumOf { it.sets }
        val week6Sets = week6Day0.items.filter { it.durationMinutes == null }.sumOf { it.sets }
        assertTrue(week6Sets < week5Sets, "Deload ($week6Sets) sollte weniger Saetze haben als die letzte Aufbauwoche ($week5Sets)")
    }

    @Test
    fun `anfaenger bekommen maximal 6 uebungen pro einheit`() {
        val program = ProgramGenerator.generate(input(experience = Experience.BEGINNER, trainingDaysWeek = 3), pool)
        for (day in program.days) {
            assertTrue(day.items.size <= 6, "Tag ${day.weekNumber}/${day.dayIndex} hat ${day.items.size} Uebungen")
        }
    }

    @Test
    fun `anfaenger nutzen ueber den gesamten mesozyklus dieselben uebungen (kein wechsel)`() {
        val program = ProgramGenerator.generate(input(experience = Experience.BEGINNER, trainingDaysWeek = 3, weeks = 6), pool)
        val week1Day0Exercises = program.days.first { it.weekNumber == 1 && it.dayIndex == 0 }.items.map { it.exerciseId }.toSet()
        val week5Day0Exercises = program.days.first { it.weekNumber == 5 && it.dayIndex == 0 }.items.map { it.exerciseId }.toSet()
        assertEquals(week1Day0Exercises, week5Day0Exercises)
    }

    @Test
    fun `anfaenger bekommen keine advanced-uebungen`() {
        val program = ProgramGenerator.generate(input(experience = Experience.BEGINNER, trainingDaysWeek = 3), pool)
        val usedIds = program.days.flatMap { it.items }.map { it.exerciseId }.toSet()
        val advancedIds = pool.filter { it.difficulty == ExerciseDifficulty.ADVANCED }.map { it.id }.toSet()
        assertTrue(usedIds.intersect(advancedIds).isEmpty())
    }

    @Test
    fun `verletzungssicherheit ist ein hartes ausschlusskriterium`() {
        val program = ProgramGenerator.generate(input(experience = Experience.ADVANCED, injuries = listOf("knee"), trainingDaysWeek = 4), pool)
        val usedIds = program.days.flatMap { it.items }.map { it.exerciseId }.toSet()
        val kneeUnsafeId = pool.first { it.slug == "bb-squat" }.id
        assertFalse(kneeUnsafeId in usedIds)
    }

    @Test
    fun `nur verfuegbares equipment wird verwendet`() {
        val program = ProgramGenerator.generate(input(equipment = listOf("bodyweight"), experience = Experience.ADVANCED, trainingDaysWeek = 3), pool)
        val usedExercises = program.days.flatMap { it.items }.map { item -> pool.first { it.id == item.exerciseId } }
        assertTrue(usedExercises.all { it.equipment.contains("bodyweight") })
    }

    @Test
    fun `kraftziel senkt den wiederholungsbereich bei verbundsuebungen`() {
        val strength = ProgramGenerator.generate(input(goal = Goal.STRENGTH, experience = Experience.ADVANCED, trainingDaysWeek = 4), pool)
        val hypertrophy = ProgramGenerator.generate(input(goal = Goal.MAINTAIN, experience = Experience.ADVANCED, trainingDaysWeek = 4), pool)
        val strengthCompoundItem = strength.days.first().items.first { it.durationMinutes == null }
        val hypertrophyCompoundItem = hypertrophy.days.first().items.first { it.durationMinutes == null }
        assertTrue(strengthCompoundItem.repMax!! <= hypertrophyCompoundItem.repMax!!)
    }

    @Test
    fun `alle sechs grundmuster sind ueber die woche durch tatsaechlich gewaehlte uebungen abgedeckt`() {
        val program = ProgramGenerator.generate(input(experience = Experience.BEGINNER, trainingDaysWeek = 3), pool)
        val week1Items = program.days.filter { it.weekNumber == 1 }.flatMap { it.items }
        val coveredPatterns = week1Items.mapNotNull { item -> pool.firstOrNull { it.id == item.exerciseId }?.pattern }.toSet()
        assertTrue(coveredPatterns.containsAll(BASE_MOVEMENT_PATTERNS))
    }

    @Test
    fun `cardio-eintraege haben eine dauer statt wiederholungen`() {
        val program = ProgramGenerator.generate(input(experience = Experience.ADVANCED, trainingDaysWeek = 3, sessionMinutes = 90), pool)
        val cardioItems = program.days.flatMap { it.items }.filter { item -> pool.first { it.id == item.exerciseId }.pattern == MovementPattern.CARDIO }
        assertTrue(cardioItems.isNotEmpty(), "Bei 90 Minuten Zeitbudget sollte ein Cardio-Slot dabei sein")
        assertTrue(cardioItems.all { it.durationMinutes != null && it.repMin == null && it.repMax == null })
    }
}
