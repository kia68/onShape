package de.optadata.odil.onshape.trainlog

import java.time.Instant
import java.util.UUID

data class WorkoutSession(
    val id: UUID,
    val userId: UUID,
    val programDayId: UUID?,
    val startedAt: Instant,
    val finishedAt: Instant?,
    val perceivedEffort: Int?,
    val notes: String?,
    val clientId: String?,
)

data class WorkoutSet(
    val id: UUID,
    val sessionId: UUID,
    val exerciseId: UUID,
    val setIndex: Int,
    val weightKg: Double?,
    val reps: Int?,
    val durationSec: Int?,
    val distanceM: Double?,
    val rir: Int?,
    val isWarmup: Boolean,
    val completed: Boolean,
    val formScore: Double?,
    val loggedAt: Instant,
    val clientId: String?,
)
