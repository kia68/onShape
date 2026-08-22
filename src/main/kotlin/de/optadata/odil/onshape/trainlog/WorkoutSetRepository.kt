package de.optadata.odil.onshape.trainlog

import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.jdbc.core.RowMapper
import org.springframework.stereotype.Repository
import java.util.UUID

/** Muss innerhalb von `RlsSession.asUser` laufen (`via_session`-Policy, V8) -- `workout_sets`
 * hat keine eigene `user_id`-Spalte, RLS prueft ueber `workout_sessions`. */
@Repository
class WorkoutSetRepository(private val jdbcTemplate: JdbcTemplate) {

    fun insert(
        sessionId: UUID,
        exerciseId: UUID,
        setIndex: Int,
        weightKg: Double?,
        reps: Int?,
        durationSec: Int?,
        distanceM: Double?,
        rir: Int?,
        isWarmup: Boolean,
        completed: Boolean,
        clientId: String?,
    ): WorkoutSet {
        val insertedId = jdbcTemplate.query(
            """
            INSERT INTO workout_sets
                (session_id, exercise_id, set_index, weight_kg, reps, duration_sec, distance_m, rir, is_warmup, completed, client_id)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
            ON CONFLICT (session_id, client_id) WHERE client_id IS NOT NULL DO NOTHING
            RETURNING id
            """.trimIndent(),
            { rs, _ -> rs.getObject("id", UUID::class.java) },
            sessionId, exerciseId, setIndex, weightKg, reps, durationSec, distanceM, rir, isWarmup, completed, clientId,
        ).firstOrNull()

        val id = insertedId
            ?: (clientId?.let { findByClientId(sessionId, it)?.id }
                ?: error("Insert into workout_sets returned no id and no client_id conflict to recover from"))
        return findById(id) ?: error("Just-inserted workout_sets row $id not found")
    }

    fun findById(id: UUID): WorkoutSet? = jdbcTemplate.query("$SELECT_SQL WHERE id = ?", rowMapper, id).firstOrNull()

    fun findByClientId(sessionId: UUID, clientId: String): WorkoutSet? =
        jdbcTemplate.query("$SELECT_SQL WHERE session_id = ? AND client_id = ?", rowMapper, sessionId, clientId).firstOrNull()

    fun findBySession(sessionId: UUID): List<WorkoutSet> =
        jdbcTemplate.query("$SELECT_SQL WHERE session_id = ? ORDER BY logged_at", rowMapper, sessionId)

    /** FR-111: ob dieser Nutzer diese Uebung jemals (in irgendeiner Session) abgeschlossen hat --
     * unabhaengig vom Gewichtsfeld, damit auch reine Koerpergewichtsuebungen (kein `weight_kg`)
     * korrekt als "schon gesehen" zaehlen (anders als [PersonalRecordDetector], das nur
     * gewichtete Saetze betrachtet). */
    fun hasEverLogged(userId: UUID, exerciseId: UUID): Boolean =
        jdbcTemplate.queryForObject(
            """
            SELECT EXISTS (
                SELECT 1 FROM workout_sets ws
                JOIN workout_sessions s ON s.id = ws.session_id
                WHERE s.user_id = ? AND ws.exercise_id = ? AND ws.completed AND NOT ws.is_warmup
            )
            """.trimIndent(),
            Boolean::class.java,
            userId, exerciseId,
        ) ?: false

    /** FR-91: letzter abgeschlossener Arbeitssatz fuer diese Uebung, aus einer FRUEHEREN Session
     * (nicht der laufenden) -- "aus der letzten Einheit". */
    fun findLastWorkingSet(userId: UUID, exerciseId: UUID, excludeSessionId: UUID): WorkoutSet? =
        jdbcTemplate.query(
            """
            $SELECT_SQL
            WHERE session_id IN (SELECT id FROM workout_sessions WHERE user_id = ?)
              AND session_id != ? AND exercise_id = ? AND completed AND NOT is_warmup
            ORDER BY logged_at DESC LIMIT 1
            """.trimIndent(),
            rowMapper, userId, excludeSessionId, exerciseId,
        ).firstOrNull()

    /** FR-97/98: alle abgeschlossenen Arbeitssaetze einer Uebung, chronologisch, fuer die
     * 1RM-Verlaufskurve und die PR-Bestwert-Berechnung ([PersonalRecordDetector]). */
    fun findWorkingSetHistory(userId: UUID, exerciseId: UUID): List<Pair<java.time.Instant, WorkingSetSample>> =
        jdbcTemplate.query(
            """
            SELECT weight_kg, reps, logged_at FROM workout_sets ws
            WHERE session_id IN (SELECT id FROM workout_sessions WHERE user_id = ?)
              AND exercise_id = ? AND completed AND NOT is_warmup AND weight_kg IS NOT NULL AND reps IS NOT NULL
            ORDER BY logged_at
            """.trimIndent(),
            { rs, _ -> rs.getTimestamp("logged_at").toInstant() to WorkingSetSample(rs.getDouble("weight_kg"), rs.getInt("reps")) },
            userId, exerciseId,
        )

    private val rowMapper = RowMapper { rs, _ ->
        WorkoutSet(
            id = rs.getObject("id", UUID::class.java),
            sessionId = rs.getObject("session_id", UUID::class.java),
            exerciseId = rs.getObject("exercise_id", UUID::class.java),
            setIndex = rs.getInt("set_index"),
            weightKg = rs.getObject("weight_kg", java.math.BigDecimal::class.java)?.toDouble(),
            reps = rs.getObject("reps", Integer::class.java) as Int?,
            durationSec = rs.getObject("duration_sec", Integer::class.java) as Int?,
            distanceM = rs.getObject("distance_m", java.math.BigDecimal::class.java)?.toDouble(),
            rir = rs.getObject("rir", Integer::class.java) as Int?,
            isWarmup = rs.getBoolean("is_warmup"),
            completed = rs.getBoolean("completed"),
            formScore = rs.getObject("form_score", java.math.BigDecimal::class.java)?.toDouble(),
            loggedAt = rs.getTimestamp("logged_at").toInstant(),
            clientId = rs.getString("client_id"),
        )
    }

    private companion object {
        const val SELECT_SQL = """
            SELECT id, session_id, exercise_id, set_index, weight_kg, reps, duration_sec, distance_m,
                   rir, is_warmup, completed, form_score, logged_at, client_id
            FROM workout_sets
        """
    }
}
