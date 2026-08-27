package de.optadata.odil.onshape.trainlog

import de.optadata.odil.onshape.web.parseEnum
import jakarta.validation.constraints.Max
import jakarta.validation.constraints.Min
import jakarta.validation.constraints.NotNull
import java.time.Instant
import java.util.UUID

data class StartSessionRequest(val programDayId: UUID?, val clientId: String?)

data class LoggedExerciseResponse(val id: UUID, val name: String)

data class FinishSessionRequest(@field:Min(1) @field:Max(10) val perceivedEffort: Int?, val notes: String?)

data class WorkoutSessionResponse(
    val id: UUID,
    val programDayId: UUID?,
    val startedAt: Instant,
    val finishedAt: Instant?,
    val perceivedEffort: Int?,
    val notes: String?,
)

fun WorkoutSession.toResponse() = WorkoutSessionResponse(id, programDayId, startedAt, finishedAt, perceivedEffort, notes)

data class WorkoutSetResponse(
    val id: UUID,
    val exerciseId: UUID,
    val setIndex: Int,
    val weightKg: Double?,
    val reps: Int?,
    val durationSec: Int?,
    val distanceM: Double?,
    val rir: Int?,
    val isWarmup: Boolean,
    val completed: Boolean,
    val loggedAt: Instant,
    val setTechnique: String?,
    val subSetIndex: Int?,
)

fun WorkoutSet.toResponse() = WorkoutSetResponse(
    id, exerciseId, setIndex, weightKg, reps, durationSec, distanceM, rir, isWarmup, completed, loggedAt,
    setTechnique?.dbValue, subSetIndex,
)

data class WorkoutSessionDetailResponse(val session: WorkoutSessionResponse, val sets: List<WorkoutSetResponse>)

data class LogSetRequest(
    @field:NotNull val exerciseId: UUID,
    @field:Min(0) val setIndex: Int,
    val weightKg: Double?,
    val reps: Int?,
    val durationSec: Int?,
    val distanceM: Double?,
    val rir: Int?,
    val isWarmup: Boolean = false,
    val completed: Boolean = true,
    val clientId: String?,
    val setTechnique: String?,
    @field:Min(0) val subSetIndex: Int?,
)

/** `setTechnique` kommt als roher String (siehe [de.optadata.odil.onshape.web.parseEnum]-Konvention). */
fun LogSetRequest.toInput() = LogSetInput(
    exerciseId, setIndex, weightKg, reps, durationSec, distanceM, rir, isWarmup, completed, clientId,
    setTechnique = setTechnique?.let { parseEnum(SetTechnique.entries, it, "setTechnique") },
    subSetIndex = subSetIndex,
)

data class PersonalRecordResponse(val type: String, val previousBest: Double?, val newValue: Double)

fun PersonalRecord.toResponse() = PersonalRecordResponse(type.name, previousBest, newValue)

data class LogSetResponse(val set: WorkoutSetResponse, val personalRecords: List<PersonalRecordResponse>)

fun LogSetResult.toResponse() = LogSetResponse(set.toResponse(), personalRecords.map { it.toResponse() })

data class WarmupSetResponse(val weightKg: Double, val reps: Int)

data class PrefillResponse(
    val lastWeightKg: Double?,
    val lastReps: Int?,
    val lastRir: Int?,
    val suggestedWeightKg: Double?,
    val suggestedReps: Int?,
    val warmupSets: List<WarmupSetResponse>,
)

fun PrefillSuggestion.toResponse() = PrefillResponse(
    lastWeightKg, lastReps, lastRir, suggestedWeightKg, suggestedReps,
    warmupSets.map { WarmupSetResponse(it.weightKg, it.reps) },
)

data class OneRepMaxPointResponse(val loggedAt: Instant, val estimated1Rm: Double)

fun OneRepMaxPoint.toResponse() = OneRepMaxPointResponse(loggedAt, estimated1Rm)

data class PersonalBestsResponse(val maxWeightKg: Double?, val maxReps: Int?, val maxEstimated1Rm: Double?, val maxSetVolume: Double?)

fun PriorBests.toResponse() = PersonalBestsResponse(maxWeightKg, maxRepsAtOrAboveWeight, maxEstimated1Rm, maxSetVolume)
