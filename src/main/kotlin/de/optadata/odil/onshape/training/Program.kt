package de.optadata.odil.onshape.training

import de.optadata.odil.onshape.onboarding.Goal
import java.util.UUID

data class ProgramItem(
    val id: UUID,
    val exerciseId: UUID,
    val sortOrder: Int,
    val sets: Int,
    val repMin: Int?,
    val repMax: Int?,
    val durationMinutes: Int?,
    val targetRir: Int?,
    val restSeconds: Int,
)

data class ProgramDay(
    val id: UUID,
    val weekNumber: Int,
    val dayIndex: Int,
    val name: String,
    val isDeload: Boolean,
    val items: List<ProgramItem>,
)

data class Program(
    val id: UUID,
    val userId: UUID?,
    val name: String,
    val goal: Goal,
    val daysPerWeek: Int,
    val weeks: Int,
    val splitType: String,
    val generatedBy: String,
    val isActive: Boolean,
    val days: List<ProgramDay>,
)

data class NewProgramItem(
    val exerciseId: UUID,
    val sortOrder: Int,
    val sets: Int,
    val repMin: Int?,
    val repMax: Int?,
    val durationMinutes: Int?,
    val targetRir: Int?,
    val restSeconds: Int,
)

data class NewProgramDay(val weekNumber: Int, val dayIndex: Int, val name: String, val isDeload: Boolean, val items: List<NewProgramItem>)

data class NewProgram(
    val name: String,
    val goal: Goal,
    val daysPerWeek: Int,
    val weeks: Int,
    val splitType: String,
    val generatedBy: String,
    val generationContext: Map<String, Any?>?,
    val days: List<NewProgramDay>,
)

/** BIZ-01 (§15.1 "Trainingsplan-Generator: 1 aktiver Plan" im Free-Tier), siehe
 * [de.optadata.odil.onshape.billing.TierPolicy.canCreateProgram] fuer die Interpretation. */
class ProgramLimitExceededException :
    RuntimeException("Free-Tier erlaubt nur ein erstelltes Programm -- auf Plus/Coach upgraden fuer weitere")
