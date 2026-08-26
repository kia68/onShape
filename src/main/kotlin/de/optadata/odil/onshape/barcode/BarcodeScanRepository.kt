package de.optadata.odil.onshape.barcode

import org.postgresql.util.PGobject
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.stereotype.Repository
import tools.jackson.databind.ObjectMapper
import java.sql.Timestamp
import java.time.Instant
import java.util.UUID

data class BarcodeScan(
    val id: UUID,
    val barcode: String,
    val foodId: UUID?,
    val found: Boolean,
    val fitScore: Int?,
    val scannedAt: Instant,
)

/** Muss innerhalb von `RlsSession.asUser(userId) { ... }` laufen (owner_only-Policy, V8). */
@Repository
class BarcodeScanRepository(
    private val jdbcTemplate: JdbcTemplate,
    private val objectMapper: ObjectMapper,
) {

    fun insert(userId: UUID, barcode: String, foodId: UUID?, found: Boolean, fitScore: Int?, breakdown: Map<String, Any?>?): UUID =
        jdbcTemplate.queryForObject(
            """
            INSERT INTO barcode_scans (user_id, barcode, food_id, found, fit_score, score_breakdown)
            VALUES (?, ?, ?, ?, ?, ?) RETURNING id
            """.trimIndent(),
            UUID::class.java,
            userId, barcode, foodId, found, fitScore, breakdown?.let { it.toJsonb() },
        ) ?: error("Insert into barcode_scans returned no id")

    /** BIZ-01: fuer den Fit-Score-Monatsdeckel im Free-Tier (siehe TierPolicy-KDoc). Zaehlt nur
     * gefundene Produkte -- ein "nicht gefunden"-Scan hat gar keinen Fit-Score, der geloggt
     * werden koennte, siehe [de.optadata.odil.onshape.barcode.BarcodeScanService]. */
    fun countFoundSince(userId: UUID, since: Instant): Int =
        jdbcTemplate.queryForObject(
            "SELECT count(*) FROM barcode_scans WHERE user_id = ? AND found AND scanned_at >= ?",
            Int::class.java, userId, Timestamp.from(since),
        ) ?: 0

    fun findRecent(userId: UUID, limit: Int): List<BarcodeScan> =
        jdbcTemplate.query(
            """
            SELECT id, barcode, food_id, found, fit_score, scanned_at FROM barcode_scans
            WHERE user_id = ? ORDER BY scanned_at DESC LIMIT ?
            """.trimIndent(),
            { rs, _ ->
                BarcodeScan(
                    id = rs.getObject("id", UUID::class.java),
                    barcode = rs.getString("barcode"),
                    foodId = rs.getObject("food_id", UUID::class.java),
                    found = rs.getBoolean("found"),
                    fitScore = rs.getObject("fit_score", Integer::class.java) as Int?,
                    scannedAt = rs.getTimestamp("scanned_at").toInstant(),
                )
            },
            userId, limit,
        )

    private fun Map<String, Any?>.toJsonb(): PGobject = PGobject().apply {
        type = "jsonb"
        value = objectMapper.writeValueAsString(this@toJsonb)
    }
}
