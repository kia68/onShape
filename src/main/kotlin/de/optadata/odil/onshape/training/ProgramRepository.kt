package de.optadata.odil.onshape.training

import de.optadata.odil.onshape.onboarding.Goal
import org.postgresql.util.PGobject
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.stereotype.Repository
import tools.jackson.databind.ObjectMapper
import java.sql.ResultSet
import java.util.UUID

/** Muss innerhalb von `RlsSession.asUser(userId) { ... }` laufen (`owner_or_template`-Policy
 * auf `programs`, `via_program`/`via_program_day` auf den Kindtabellen, V8). */
@Repository
class ProgramRepository(
    private val jdbcTemplate: JdbcTemplate,
    private val objectMapper: ObjectMapper,
) {

    /** FR-70: eine neue generierte/manuelle Version wird aktiv, die vorherige inaktiv --
     * `programs.user_id` behaelt aber die Historie (kein Loeschen). */
    fun insert(userId: UUID, program: NewProgram): UUID {
        jdbcTemplate.update("UPDATE programs SET is_active = false WHERE user_id = ? AND is_active", userId)

        val programId = jdbcTemplate.queryForObject(
            """
            INSERT INTO programs (user_id, name_de, goal, days_per_week, weeks, split_type, generated_by, generation_ctx, is_active)
            VALUES (?, ?, ?::goal_t, ?, ?, ?, ?, ?, true) RETURNING id
            """.trimIndent(),
            UUID::class.java,
            userId, program.name, program.goal.dbValue, program.daysPerWeek, program.weeks, program.splitType,
            program.generatedBy, program.generationContext?.let { it.toJsonb() },
        ) ?: error("Insert into programs returned no id")

        for (day in program.days) {
            val dayId = jdbcTemplate.queryForObject(
                "INSERT INTO program_days (program_id, week_number, day_index, name_de, is_deload) VALUES (?, ?, ?, ?, ?) RETURNING id",
                UUID::class.java,
                programId, day.weekNumber, day.dayIndex, day.name, day.isDeload,
            ) ?: error("Insert into program_days returned no id")

            for (item in day.items) {
                jdbcTemplate.update(
                    """
                    INSERT INTO program_items (program_day_id, exercise_id, sort_order, sets, rep_min, rep_max, duration_minutes, target_rir, rest_seconds)
                    VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)
                    """.trimIndent(),
                    dayId, item.exerciseId, item.sortOrder, item.sets, item.repMin, item.repMax,
                    item.durationMinutes, item.targetRir, item.restSeconds,
                )
            }
        }
        return programId
    }

    /** BIZ-01: Gesamtzahl je Account (nicht nur aktive), fuer den Free-Tier-Deckel
     * "1 aktiver Plan" (siehe TierPolicy-KDoc). */
    fun countForUser(userId: UUID): Int =
        jdbcTemplate.queryForObject("SELECT count(*) FROM programs WHERE user_id = ?", Int::class.java, userId) ?: 0

    fun findActiveByUser(userId: UUID): Program? {
        val programId = jdbcTemplate.query(
            "SELECT id FROM programs WHERE user_id = ? AND is_active LIMIT 1",
            { rs, _ -> rs.getObject("id", UUID::class.java) },
            userId,
        ).firstOrNull() ?: return null
        return findById(programId)
    }

    /** FR-137 (Datenexport): alle eigenen Programme (aktiv + Historie), NICHT die
     * userlosen Vorlagen (`user_id IS NULL`). */
    fun findAllForUser(userId: UUID): List<Program> =
        jdbcTemplate.query(
            "SELECT id FROM programs WHERE user_id = ? ORDER BY created_at",
            { rs, _ -> rs.getObject("id", UUID::class.java) },
            userId,
        ).mapNotNull { findById(it) }

    fun findById(id: UUID): Program? {
        val header = jdbcTemplate.query(HEADER_SQL, { rs, _ -> rs.toHeader() }, id).firstOrNull() ?: return null
        val days = jdbcTemplate.query(
            "SELECT id, week_number, day_index, name_de, is_deload FROM program_days WHERE program_id = ? ORDER BY week_number, day_index",
            { rs, _ -> rs.getObject("id", UUID::class.java) to ProgramDayHeader(rs.getInt("week_number"), rs.getInt("day_index"), rs.getString("name_de"), rs.getBoolean("is_deload")) },
            id,
        )
        if (days.isEmpty()) return header.toProgram(emptyList())

        val itemsByDay = findItems(days.map { it.first })
        val fullDays = days.map { (dayId, d) -> ProgramDay(dayId, d.weekNumber, d.dayIndex, d.name, d.isDeload, itemsByDay[dayId] ?: emptyList()) }
        return header.toProgram(fullDays)
    }

    fun setActive(userId: UUID, programId: UUID) {
        jdbcTemplate.update("UPDATE programs SET is_active = false WHERE user_id = ? AND is_active", userId)
        jdbcTemplate.update("UPDATE programs SET is_active = true WHERE id = ? AND user_id = ?", programId, userId)
    }

    private fun findItems(dayIds: List<UUID>): Map<UUID, List<ProgramItem>> {
        if (dayIds.isEmpty()) return emptyMap()
        val placeholders = dayIds.joinToString(",") { "?" }
        return jdbcTemplate.query(
            """
            SELECT id, program_day_id, exercise_id, sort_order, sets, rep_min, rep_max, duration_minutes, target_rir, rest_seconds
            FROM program_items WHERE program_day_id IN ($placeholders) ORDER BY sort_order
            """.trimIndent(),
            { rs, _ ->
                rs.getObject("program_day_id", UUID::class.java) to ProgramItem(
                    id = rs.getObject("id", UUID::class.java),
                    exerciseId = rs.getObject("exercise_id", UUID::class.java),
                    sortOrder = rs.getInt("sort_order"),
                    sets = rs.getInt("sets"),
                    repMin = rs.getObject("rep_min", Integer::class.java) as Int?,
                    repMax = rs.getObject("rep_max", Integer::class.java) as Int?,
                    durationMinutes = rs.getObject("duration_minutes", Integer::class.java) as Int?,
                    targetRir = rs.getObject("target_rir", Integer::class.java) as Int?,
                    restSeconds = rs.getInt("rest_seconds"),
                )
            },
            *dayIds.toTypedArray(),
        ).groupBy({ it.first }, { it.second })
    }

    /** FR-74: eine einzelne Uebung innerhalb eines aktiven Programms ersetzen -- in ALLEN
     * Wochen, in denen sie unter demselben `program_day.day_index` vorkommt (konsistent mit
     * der "kein Wechsel innerhalb des Mesozyklus"-Logik des Generators: der Tausch gilt fuer
     * den ganzen Block, nicht nur eine einzelne Woche). */
    fun replaceExerciseInProgram(programId: UUID, oldExerciseId: UUID, newExerciseId: UUID): Int =
        jdbcTemplate.update(
            """
            UPDATE program_items SET exercise_id = ?
            WHERE exercise_id = ? AND program_day_id IN (SELECT id FROM program_days WHERE program_id = ?)
            """.trimIndent(),
            newExerciseId, oldExerciseId, programId,
        )

    private fun ResultSet.toHeader() = ProgramHeader(
        id = getObject("id", UUID::class.java),
        userId = getObject("user_id", UUID::class.java),
        name = getString("name_de"),
        goal = Goal.entries.first { it.dbValue == getString("goal") },
        daysPerWeek = getInt("days_per_week"),
        weeks = getInt("weeks"),
        splitType = getString("split_type"),
        generatedBy = getString("generated_by"),
        isActive = getBoolean("is_active"),
    )

    private fun Map<String, Any?>.toJsonb(): PGobject = PGobject().apply {
        type = "jsonb"
        value = objectMapper.writeValueAsString(this@toJsonb)
    }

    private data class ProgramDayHeader(val weekNumber: Int, val dayIndex: Int, val name: String, val isDeload: Boolean)

    private data class ProgramHeader(
        val id: UUID, val userId: UUID?, val name: String, val goal: Goal, val daysPerWeek: Int,
        val weeks: Int, val splitType: String, val generatedBy: String, val isActive: Boolean,
    ) {
        fun toProgram(days: List<ProgramDay>) = Program(id, userId, name, goal, daysPerWeek, weeks, splitType, generatedBy, isActive, days)
    }

    private companion object {
        const val HEADER_SQL = """
            SELECT id, user_id, name_de, goal, days_per_week, weeks, split_type, generated_by, is_active
            FROM programs WHERE id = ?
        """
    }
}
