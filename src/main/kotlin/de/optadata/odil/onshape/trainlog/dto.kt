package de.optadata.odil.onshape.trainlog

import jakarta.validation.constraints.Max
import jakarta.validation.constraints.Min
import jakarta.validation.constraints.NotNull
import java.time.Instant
import java.util.UUID

data class StartSessionRequest(val programDayId: UUID?, val clientId: String?)

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
)

fun WorkoutSet.toResponse() = WorkoutSetResponse(id, exerciseId, setIndex, weightKg, reps, durationSec, distanceM, rir, isWarmup, completed, loggedAt)

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
)

fun LogSetRequest.toInput() = LogSetInput(exerciseId, setIndex, weightKg, reps, durationSec, distanceM, rir, isWarmup, completed, clientId)

data class PersonalRecordResponse(val type: String, val previousBest: Double?, val newValue: Double)

fun PersonalRecord.toResponse() = PersonalRecordResponse(type.name, previousBest, newValue)

data class LogSetResponse(val set: WorkoutSetResponse, val personalRecords: List<PersonalRecordResponse>)

fun LogSetResult.toResponse() = LogSetResponse(set.toResponse(), personalRecords.map { it.toResponse() })

data class PrefillResponse(
    val lastWeightKg: Double?,
    val lastReps: Int?,
    val lastRir: Int?,
    val suggestedWeightKg: Double?,
    val suggestedReps: Int?,
)

fun PrefillSuggestion.toResponse() = PrefillResponse(lastWeightKg, lastReps, lastRir, suggestedWeightKg, suggestedReps)

data class OneRepMaxPointResponse(val loggedAt: Instant, val estimated1Rm: Double)

fun OneRepMaxPoint.toResponse() = OneRepMaxPointResponse(loggedAt, estimated1Rm)

data class PersonalBestsResponse(val maxWeightKg: Double?, val maxReps: Int?, val maxEstimated1Rm: Double?, val maxSetVolume: Double?)

fun PriorBests.toResponse() = PersonalBestsResponse(maxWeightKg, maxRepsAtOrAboveWeight, maxEstimated1Rm, maxSetVolume)
