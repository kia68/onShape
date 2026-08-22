package de.optadata.odil.onshape.trainlog

import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.jdbc.core.RowMapper
import org.springframework.stereotype.Repository
import java.util.UUID

/** Muss innerhalb von `RlsSession.asUser` laufen (owner_only, V8). Idempotenz-Muster fuer
 * Offline-Start identisch zu [de.optadata.odil.onshape.nutrition.WaterEntryRepository.insert]. */
@Repository
class WorkoutSessionRepository(private val jdbcTemplate: JdbcTemplate) {

    fun start(userId: UUID, programDayId: UUID?, clientId: String?): WorkoutSession {
        val insertedId = jdbcTemplate.query(
            """
            INSERT INTO workout_sessions (user_id, program_day_id, started_at, client_id)
            VALUES (?, ?, now(), ?)
            ON CONFLICT (user_id, client_id) WHERE client_id IS NOT NULL DO NOTHING
            RETURNING id
            """.trimIndent(),
            { rs, _ -> rs.getObject("id", UUID::class.java) },
            userId, programDayId, clientId,
        ).firstOrNull()

        val id = insertedId
            ?: (clientId?.let { findByClientId(userId, it)?.id }
                ?: error("Insert into workout_sessions returned no id and no client_id conflict to recover from"))
        return findById(id) ?: error("Just-inserted workout_sessions row $id not found")
    }

    fun finish(userId: UUID, id: UUID, perceivedEffort: Int?, notes: String?): WorkoutSession? {
        val updated = jdbcTemplate.update(
            "UPDATE workout_sessions SET finished_at = now(), perceived_effort = ?, notes = ? WHERE id = ? AND user_id = ? AND finished_at IS NULL",
            perceivedEffort, notes, id, userId,
        )
        if (updated == 0) return null
        return findById(id)
    }

    fun findById(id: UUID): WorkoutSession? = jdbcTemplate.query("$SELECT_SQL WHERE id = ?", rowMapper, id).firstOrNull()

    fun findByClientId(userId: UUID, clientId: String): WorkoutSession? =
        jdbcTemplate.query("$SELECT_SQL WHERE user_id = ? AND client_id = ?", rowMapper, userId, clientId).firstOrNull()

    fun findActiveByUser(userId: UUID): WorkoutSession? =
        jdbcTemplate.query(
            "$SELECT_SQL WHERE user_id = ? AND finished_at IS NULL ORDER BY started_at DESC LIMIT 1",
            rowMapper, userId,
        ).firstOrNull()

    /** FR-137 (Datenexport): wirklich ALLE Sessions, auch eine gerade laufende (anders als
     * [findHistory], das bewusst nur beendete Workouts fuer die Verlaufsansicht liefert). */
    fun findAllForUser(userId: UUID): List<WorkoutSession> =
        jdbcTemplate.query("$SELECT_SQL WHERE user_id = ? ORDER BY started_at", rowMapper, userId)

    fun findHistory(userId: UUID, limit: Int): List<WorkoutSession> =
        jdbcTemplate.query(
            "$SELECT_SQL WHERE user_id = ? AND finished_at IS NOT NULL ORDER BY started_at DESC LIMIT ?",
            rowMapper, userId, limit,
        )

    private val rowMapper = RowMapper { rs, _ ->
        WorkoutSession(
            id = rs.getObject("id", UUID::class.java),
            userId = rs.getObject("user_id", UUID::class.java),
            programDayId = rs.getObject("program_day_id", UUID::class.java),
            startedAt = rs.getTimestamp("started_at").toInstant(),
            finishedAt = rs.getTimestamp("finished_at")?.toInstant(),
            perceivedEffort = rs.getObject("perceived_effort", Integer::class.java) as Int?,
            notes = rs.getString("notes"),
            clientId = rs.getString("client_id"),
        )
    }

    private companion object {
        const val SELECT_SQL =
            "SELECT id, user_id, program_day_id, started_at, finished_at, perceived_effort, notes, client_id FROM workout_sessions"
    }
}
