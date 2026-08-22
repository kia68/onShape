package de.optadata.odil.onshape.training

import de.optadata.odil.onshape.onboarding.Experience
import de.optadata.odil.onshape.onboarding.Goal
import java.util.UUID
import kotlin.math.roundToInt

data class ProgramGeneratorInput(
    val goal: Goal,
    val experience: Experience,
    val age: Int,
    val equipment: List<String>,
    val injuries: List<String>,
    val trainingDaysWeek: Int,
    val sessionMinutes: Int,
    val weeks: Int = 6,
    val splitTypeOverride: String? = null,
)

data class GeneratedItem(
    val exerciseId: UUID,
    val sortOrder: Int,
    val sets: Int,
    val repMin: Int?,
    val repMax: Int?,
    val durationMinutes: Int?,
    val targetRir: Int,
    val restSeconds: Int,
)

data class GeneratedDay(val weekNumber: Int, val dayIndex: Int, val nameKey: String, val label: String, val isDeload: Boolean, val items: List<GeneratedItem>)

data class GeneratedProgram(val splitType: String, val weeks: Int, val daysPerWeek: Int, val days: List<GeneratedDay>)

/**
 * §7.4: orchestriert Split (Schritt 2), Uebungsauswahl (Schritt 3) und Volumen-/RIR-Progression
 * (Schritte 1+4) zu einem vollstaendigen Mesozyklus. Rein aus Eingaben berechnet, keine
 * Datenbankzugriffe -- dadurch ohne Testcontainer testbar (NFR-13 "Progression").
 */
object ProgramGenerator {
    private const val BEGINNER_MAX_EXERCISES_PER_SESSION = 6
    private const val MINUTES_PER_EXERCISE_SLOT = 8
    private const val MIN_EXERCISES_PER_SESSION = 3
    private const val MAX_EXERCISES_PER_SESSION = 10
    private const val MIN_SETS_PER_EXERCISE = 2
    private const val MAX_SETS_PER_EXERCISE = 6
    private const val CARDIO_MIN_MINUTES = 10
    private const val CARDIO_DEFAULT_MINUTES = 15
    private const val REST_SECONDS_CORE = 45
    private const val REST_SECONDS_COMPOUND = 120
    private const val REST_SECONDS_ISOLATION = 60

    fun generate(input: ProgramGeneratorInput, pool: List<Exercise>, rejectionCounts: Map<UUID, Int> = emptyMap()): GeneratedProgram {
        val isBeginner = input.experience == Experience.NONE || input.experience == Experience.BEGINNER
        val forceFullBody = isBeginner || VolumeCorridor.preferHighFrequencySplit(input.age)
        val split = SplitAssigner.assign(input.trainingDaysWeek, input.experience, forceFullBody, input.splitTypeOverride)
        val maxDifficulty = if (isBeginner) ExerciseDifficulty.BEGINNER else ExerciseDifficulty.ADVANCED
        val maxExercisesPerSession = if (isBeginner) {
            BEGINNER_MAX_EXERCISES_PER_SESSION
        } else {
            (input.sessionMinutes / MINUTES_PER_EXERCISE_SLOT).coerceIn(MIN_EXERCISES_PER_SESSION, MAX_EXERCISES_PER_SESSION)
        }
        val equipment = input.equipment.toSet()
        val injuries = input.injuries.toSet()
        val includeCardio = input.sessionMinutes >= (maxExercisesPerSession * MINUTES_PER_EXERCISE_SLOT + CARDIO_MIN_MINUTES)

        // Uebungsauswahl einmal pro Tagesvorlage, ueber den GESAMTEN Mesozyklus stabil (§7.4
        // Schritt 3 Anfaenger-Regel "kein Uebungswechsel in den ersten 4 Wochen" -- hier auf den
        // ganzen generierten Block ausgeweitet, siehe Klassendoku).
        val daySelections: List<List<Exercise>> = if (isBeginner) {
            val template = split.days.first()
            val picks = selectExercisesForDay(template, pool, maxDifficulty, equipment, injuries, emptySet(), rejectionCounts, maxExercisesPerSession, includeCardio)
            split.days.map { picks }
        } else {
            val used = mutableSetOf<UUID>()
            split.days.map { day ->
                val picks = selectExercisesForDay(day, pool, maxDifficulty, equipment, injuries, used, rejectionCounts, maxExercisesPerSession, includeCardio)
                used += picks.map { it.id }
                picks
            }
        }

        val corridor = VolumeCorridor.forProfile(input.experience, input.age)
        val stages = MesocycleProgression.stagesFor(corridor, input.weeks)
        val patternFrequency = patternFrequencyPerWeek(split)

        val days = stages.flatMap { stage ->
            split.days.mapIndexed { dayIndex, template ->
                val exercises = daySelections[dayIndex]
                val items = exercises.mapIndexed { sortOrder, exercise ->
                    buildItem(exercise, sortOrder, stage, patternFrequency[exercise.pattern] ?: 1, input.goal)
                }
                GeneratedDay(stage.weekNumber, dayIndex, template.nameKey, template.label, stage.isDeload, items)
            }
        }

        return GeneratedProgram(split.splitType, input.weeks, split.days.size, days)
    }

    private fun selectExercisesForDay(
        day: DayTemplate,
        pool: List<Exercise>,
        maxDifficulty: ExerciseDifficulty,
        equipment: Set<String>,
        injuries: Set<String>,
        globallyUsed: Set<UUID>,
        rejectionCounts: Map<UUID, Int>,
        maxExercises: Int,
        includeCardio: Boolean,
    ): List<Exercise> {
        val selected = mutableListOf<Exercise>()
        val selectedMuscles = mutableSetOf<String>()

        // Die sechs Grundmuster haben Vorrang (§7.4 Schritt 3, "wichtigste Nebenbedingung"),
        // Rumpf danach, Cardio zuletzt und nur wenn noch Zeitbudget uebrig ist.
        val slots = day.patterns + (if (day.includeCore) listOf(MovementPattern.CORE) else emptyList())
        for (pattern in slots) {
            if (selected.size >= maxExercises) break
            val context = ScoringContext(equipment, injuries, maxDifficulty, selectedMuscles, globallyUsed + selected.map { it.id }, rejectionCounts)
            val pick = ExerciseScorer.pickBest(pattern, pool, context) ?: continue
            selected += pick
            selectedMuscles += pick.primaryMuscles
        }
        if (includeCardio && selected.size < maxExercises) {
            val context = ScoringContext(equipment, injuries, maxDifficulty, selectedMuscles, globallyUsed + selected.map { it.id }, rejectionCounts)
            ExerciseScorer.pickBest(MovementPattern.CARDIO, pool, context)?.let { selected += it }
        }
        return selected
    }

    private fun patternFrequencyPerWeek(split: SplitPlan): Map<MovementPattern, Int> {
        val counts = mutableMapOf<MovementPattern, Int>()
        for (day in split.days) {
            for (pattern in day.patterns) counts[pattern] = (counts[pattern] ?: 0) + 1
            if (day.includeCore) counts[MovementPattern.CORE] = (counts[MovementPattern.CORE] ?: 0) + 1
        }
        // Cardio ist keine Tagesvorlagen-Eigenschaft (siehe includeCardio-Zusatzslot oben) --
        // ohne Frequenz-Eintrag greift der Fallback "1" in buildItem, d.h. volles Wochenziel
        // pro Session (Cardio wird nicht auf mehrere Tage verteilt gedacht).
        return counts
    }

    private fun buildItem(exercise: Exercise, sortOrder: Int, stage: WeekStage, patternFrequency: Int, goal: Goal): GeneratedItem {
        if (exercise.pattern == MovementPattern.CARDIO) {
            val minutes = if (stage.isDeload) (CARDIO_DEFAULT_MINUTES * 0.5).roundToInt() else CARDIO_DEFAULT_MINUTES
            return GeneratedItem(exercise.id, sortOrder, sets = 1, repMin = null, repMax = null, durationMinutes = minutes, targetRir = stage.targetRir, restSeconds = 0)
        }
        val sets = (stage.setsPerMuscle.toDouble() / patternFrequency).roundToInt().coerceIn(MIN_SETS_PER_EXERCISE, MAX_SETS_PER_EXERCISE)
        val reps = repRange(goal, exercise.mechanic)
        val rest = when {
            exercise.pattern == MovementPattern.CORE -> REST_SECONDS_CORE
            exercise.mechanic == Mechanic.COMPOUND -> REST_SECONDS_COMPOUND
            else -> REST_SECONDS_ISOLATION
        }
        return GeneratedItem(exercise.id, sortOrder, sets, reps.first, reps.last, null, stage.targetRir, rest)
    }

    /** KONZEPT.md §7.4 Schritt 4, Absatz zu `goal = 'strength'`: "gewichtet die Intensitaet und
     * die Frequenz hoeher als das Volumen" -- hier als niedrigerer Wiederholungsbereich
     * (naeher an 1RM) fuer Verbundsuebungen umgesetzt. Isolationsuebungen bleiben unveraendert,
     * weil sie fuer reine Kraftziele nur eine untergeordnete Rolle spielen. */
    private fun repRange(goal: Goal, mechanic: Mechanic): IntRange = when {
        goal == Goal.STRENGTH && mechanic == Mechanic.COMPOUND -> 3..6
        mechanic == Mechanic.COMPOUND -> 6..10
        else -> 10..15
    }
}
