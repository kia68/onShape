package de.optadata.odil.onshape.training

import de.optadata.odil.onshape.onboarding.EnumWithDbValue
import java.util.UUID

/** Spiegelt `movement_pattern_t` aus V4__exercises.sql. */
enum class MovementPattern(override val dbValue: String) : EnumWithDbValue {
    SQUAT("squat"), HINGE("hinge"), PUSH_HORIZONTAL("push_horizontal"), PUSH_VERTICAL("push_vertical"),
    PULL_HORIZONTAL("pull_horizontal"), PULL_VERTICAL("pull_vertical"), CARRY("carry"),
    CORE("core"), ISOLATION("isolation"), CARDIO("cardio"),
}

/** Die sechs Grundmuster aus §7.4 Schritt 3, die jeder Plan abdecken muss (plus Rumpf/einbeinig,
 * separat behandelt). */
val BASE_MOVEMENT_PATTERNS = listOf(
    MovementPattern.SQUAT, MovementPattern.HINGE, MovementPattern.PUSH_HORIZONTAL,
    MovementPattern.PUSH_VERTICAL, MovementPattern.PULL_HORIZONTAL, MovementPattern.PULL_VERTICAL,
)

enum class Mechanic(override val dbValue: String) : EnumWithDbValue {
    COMPOUND("compound"), ISOLATION("isolation"),
}

enum class ExerciseDifficulty(override val dbValue: String) : EnumWithDbValue {
    BEGINNER("beginner"), INTERMEDIATE("intermediate"), ADVANCED("advanced");

    /** Fuer die Anfaenger-Hartregel "erst Maschine/geführt, dann Kurzhantel, dann Langhantel"
     * (§7.4 Schritt 3) -- vereinfacht ueber die Schwierigkeitsstufe statt ueber eine
     * uebungsspezifische Ausruestungs-Rangfolge abgebildet, siehe [ProgramGenerator]. */
    fun atMost(other: ExerciseDifficulty) = ordinal <= other.ordinal
}

data class ExerciseMuscleFactor(val muscle: String, val factor: Double)

data class Exercise(
    val id: UUID,
    val slug: String,
    val name: String,
    val pattern: MovementPattern,
    val mechanic: Mechanic,
    val equipment: List<String>,
    val difficulty: ExerciseDifficulty,
    val unilateral: Boolean,
    val metValue: Double?,
    val contraindications: List<String>,
    val muscles: List<ExerciseMuscleFactor>,
) {
    val primaryMuscles: List<String> get() = muscles.filter { it.factor >= 1.0 }.map { it.muscle }
}
