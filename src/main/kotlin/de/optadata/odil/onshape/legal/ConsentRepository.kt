package de.optadata.odil.onshape.legal

import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.stereotype.Repository
import java.sql.ResultSet
import java.sql.Timestamp
import java.time.Instant
import java.util.UUID

/** Muss innerhalb von `RlsSession.asUser(userId) { ... }` laufen (owner_only-Policy, V16). */
@Repository
class ConsentRepository(private val jdbcTemplate: JdbcTemplate) {

    fun findAllForUser(userId: UUID): List<Consent> =
        jdbcTemplate.query(SELECT_SQL, { rs, _ -> rs.toConsent() }, userId)

    /** Ein Zweck ist immer eine vollstaendige Entscheidung (erteilt oder widerrufen), nie ein
     * Update auf eine unvollstaendige Zeile -- `granted_at`/`revoked_at` spiegeln jeweils den
     * Zeitpunkt DIESER Entscheidung, die jeweils andere Spalte wird bewusst genullt. */
    fun upsert(userId: UUID, purpose: ConsentPurpose, granted: Boolean, at: Instant) {
        jdbcTemplate.update(
            """
            INSERT INTO user_consents (user_id, purpose, granted, granted_at, revoked_at, updated_at)
            VALUES (?, ?::consent_purpose_t, ?, ?, ?, ?)
            ON CONFLICT (user_id, purpose) DO UPDATE SET
                granted = EXCLUDED.granted, granted_at = EXCLUDED.granted_at,
                revoked_at = EXCLUDED.revoked_at, updated_at = EXCLUDED.updated_at
            """.trimIndent(),
            userId, purpose.dbValue, granted,
            if (granted) Timestamp.from(at) else null,
            if (!granted) Timestamp.from(at) else null,
            Timestamp.from(at),
        )
    }

    private fun ResultSet.toConsent() = Consent(
        purpose = ConsentPurpose.entries.first { it.dbValue == getString("purpose") },
        granted = getBoolean("granted"),
        grantedAt = getTimestamp("granted_at")?.toInstant(),
        revokedAt = getTimestamp("revoked_at")?.toInstant(),
        updatedAt = getTimestamp("updated_at")?.toInstant(),
    )

    private companion object {
        const val SELECT_SQL = "SELECT purpose, granted, granted_at, revoked_at, updated_at FROM user_consents WHERE user_id = ?"
    }
}
