package de.optadata.odil.onshape.nutrition

import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.stereotype.Repository
import java.time.LocalDate
import java.util.UUID

/** FR-29. Muss innerhalb von `RlsSession.asUser` laufen (owner_only, V10). Idempotenz-Muster
 * fuer Offline-Sync (FR-31) identisch zu [FoodEntryRepository.insert]. */
@Repository
class WaterEntryRepository(private val jdbcTemplate: JdbcTemplate) {

    fun insert(userId: UUID, loggedDate: LocalDate, amountMl: Int, clientId: String?): WaterEntry {
        val insertedId = jdbcTemplate.query(
            """
            INSERT INTO water_entries (user_id, logged_date, amount_ml, client_id) VALUES (?, ?, ?, ?)
            ON CONFLICT (user_id, client_id) WHERE client_id IS NOT NULL DO NOTHING
            RETURNING id
            """.trimIndent(),
            { rs, _ -> rs.getObject("id", UUID::class.java) },
            userId, loggedDate, amountMl, clientId,
        ).firstOrNull()

        val id = insertedId
            ?: (clientId?.let { findByClientId(userId, it)?.id }
                ?: error("Insert into water_entries returned no id and no client_id conflict to recover from"))
        return findById(id) ?: error("Just-inserted water_entries row $id not found")
    }

    fun findById(id: UUID) =
        jdbcTemplate.query(SELECT_SQL + " WHERE id = ?", rowMapper, id).firstOrNull()

    fun findByClientId(userId: UUID, clientId: String) =
        jdbcTemplate.query(SELECT_SQL + " WHERE user_id = ? AND client_id = ?", rowMapper, userId, clientId).firstOrNull()

    fun findByDate(userId: UUID, date: LocalDate): List<WaterEntry> =
        jdbcTemplate.query(SELECT_SQL + " WHERE user_id = ? AND logged_date = ? ORDER BY created_at", rowMapper, userId, date)

    /** FR-137 (Datenexport). */
    fun findAllForUser(userId: UUID): List<WaterEntry> =
        jdbcTemplate.query(SELECT_SQL + " WHERE user_id = ? ORDER BY logged_date", rowMapper, userId)

    fun delete(userId: UUID, id: UUID): Boolean =
        jdbcTemplate.update("DELETE FROM water_entries WHERE id = ? AND user_id = ?", id, userId) > 0

    private val rowMapper = org.springframework.jdbc.core.RowMapper { rs, _ ->
        WaterEntry(
            id = rs.getObject("id", UUID::class.java),
            userId = rs.getObject("user_id", UUID::class.java),
            loggedDate = rs.getObject("logged_date", LocalDate::class.java),
            amountMl = rs.getInt("amount_ml"),
            clientId = rs.getString("client_id"),
            createdAt = rs.getTimestamp("created_at").toInstant(),
        )
    }

    private companion object {
        const val SELECT_SQL = "SELECT id, user_id, logged_date, amount_ml, client_id, created_at FROM water_entries"
    }
}
