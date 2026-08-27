package de.optadata.odil.onshape.training

import de.optadata.odil.onshape.onboarding.Goal
import jakarta.validation.Valid
import jakarta.validation.constraints.Max
import jakarta.validation.constraints.Min
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotEmpty
import jakarta.validation.constraints.NotNull
import java.util.UUID

data class GenerateProgramRequest(@field:Min(2) @field:Max(12) val weeks: Int = 6, val splitTypeOverride: String? = null)

data class ManualProgramItemRequest(
    @field:NotNull val exerciseId: UUID,
    @field:Min(0) val sortOrder: Int,
    @field:Min(1) val sets: Int,
    val repMin: Int?,
    val repMax: Int?,
    val durationMinutes: Int?,
    val targetRir: Int?,
    @field:Min(0) val restSeconds: Int,
    val supersetGroup: Int? = null,
)

data class ManualProgramDayRequest(
    @field:Min(1) val weekNumber: Int,
    @field:Min(0) val dayIndex: Int,
    @field:NotBlank val name: String,
    val isDeload: Boolean = false,
    @field:NotEmpty @field:Valid val items: List<ManualProgramItemRequest>,
)

data class ManualProgramRequest(
    @field:NotBlank val name: String,
    @field:NotBlank val goal: String,
    @field:Min(1) @field:Max(7) val daysPerWeek: Int,
    @field:Min(1) @field:Max(12) val weeks: Int,
    @field:NotBlank val splitType: String,
    @field:NotEmpty @field:Valid val days: List<ManualProgramDayRequest>,
)

/** `goal` kommt als roher String (siehe [de.optadata.odil.onshape.web.parseEnum]-Konvention) --
 * der Aufrufer muss ihn vorher validiert haben. */
fun ManualProgramRequest.toNewProgram(goal: Goal) = NewProgram(
    name = name,
    goal = goal,
    daysPerWeek = daysPerWeek,
    weeks = weeks,
    splitType = splitType,
    generatedBy = "manual",
    generationContext = null,
    days = days.map { day ->
        NewProgramDay(
            weekNumber = day.weekNumber,
            dayIndex = day.dayIndex,
            name = day.name,
            isDeload = day.isDeload,
            items = day.items.map { item ->
                NewProgramItem(
                    item.exerciseId, item.sortOrder, item.sets, item.repMin, item.repMax, item.durationMinutes,
                    item.targetRir, item.restSeconds, item.supersetGroup,
                )
            },
        )
    },
)

data class ProgramItemResponse(
    val id: UUID,
    val exerciseId: UUID,
    val exerciseName: String,
    val sortOrder: Int,
    val sets: Int,
    val repMin: Int?,
    val repMax: Int?,
    val durationMinutes: Int?,
    val targetRir: Int?,
    val restSeconds: Int,
    val supersetGroup: Int?,
)

data class ProgramDayResponse(
    val id: UUID,
    val weekNumber: Int,
    val dayIndex: Int,
    val name: String,
    val isDeload: Boolean,
    val items: List<ProgramItemResponse>,
)

data class ProgramResponse(
    val id: UUID,
    val name: String,
    val goal: String,
    val daysPerWeek: Int,
    val weeks: Int,
    val splitType: String,
    val generatedBy: String,
    val isActive: Boolean,
    val days: List<ProgramDayResponse>,
)

fun Program.toResponse(namesById: Map<UUID, String>) = ProgramResponse(
    id = id,
    name = name,
    goal = goal.dbValue,
    daysPerWeek = daysPerWeek,
    weeks = weeks,
    splitType = splitType,
    generatedBy = generatedBy,
    isActive = isActive,
    days = days.map { day ->
        ProgramDayResponse(
            id = day.id,
            weekNumber = day.weekNumber,
            dayIndex = day.dayIndex,
            name = day.name,
            isDeload = day.isDeload,
            items = day.items.map { item ->
                ProgramItemResponse(
                    id = item.id,
                    exerciseId = item.exerciseId,
                    exerciseName = namesById[item.exerciseId] ?: item.exerciseId.toString(),
                    sortOrder = item.sortOrder,
                    sets = item.sets,
                    repMin = item.repMin,
                    repMax = item.repMax,
                    durationMinutes = item.durationMinutes,
                    targetRir = item.targetRir,
                    restSeconds = item.restSeconds,
                    supersetGroup = item.supersetGroup,
                )
            },
        )
    },
)

data class ExerciseResponse(
    val id: UUID,
    val slug: String,
    val name: String,
    val pattern: String,
    val mechanic: String,
    val equipment: List<String>,
    val difficulty: String,
    val unilateral: Boolean,
)

fun Exercise.toResponse() = ExerciseResponse(id, slug, name, pattern.dbValue, mechanic.dbValue, equipment, difficulty.dbValue, unilateral)

data class SwapExerciseRequest(@field:NotBlank val reason: String)

data class SwapExerciseResponse(val program: ProgramResponse, val replacementExerciseId: UUID, val replacementExerciseName: String)

data class MuscleVolumeEntryResponse(val muscle: String, val plannedSets: Double, val corridorMin: Int, val corridorMax: Int, val status: String)

data class VolumeDashboardResponse(val weekNumber: Int, val isDeload: Boolean, val entries: List<MuscleVolumeEntryResponse>)

fun VolumeDashboard.toResponse() = VolumeDashboardResponse(
    weekNumber = weekNumber,
    isDeload = isDeload,
    entries = entries.map { MuscleVolumeEntryResponse(it.muscle, it.plannedSets, it.corridorMin, it.corridorMax, it.status.name) },
)
