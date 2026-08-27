package de.optadata.odil.onshape.trainlog

import de.optadata.odil.onshape.onboarding.EnumWithDbValue
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

/** FR-95: Drop-/Cluster-Satz-Markierung auf [WorkoutSet]. Ein einfacher Satz hat kein
 * [WorkoutSet.setTechnique]; ALLE Zeilen einer Gruppe (auch der Hauptsatz) tragen dieselbe
 * Technik + einen ab 0 hochzaehlenden [WorkoutSet.subSetIndex] -- siehe V20-Migrationskommentar. */
enum class SetTechnique(override val dbValue: String) : EnumWithDbValue {
    DROPSET("dropset"),
    CLUSTER("cluster"),
}

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
    val setTechnique: SetTechnique?,
    val subSetIndex: Int?,
)
