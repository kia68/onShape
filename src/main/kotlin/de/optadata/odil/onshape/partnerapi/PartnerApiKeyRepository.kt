package de.optadata.odil.onshape.partnerapi

import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.stereotype.Repository
import java.sql.ResultSet
import java.sql.Timestamp
import java.time.Instant
import java.util.UUID

/** `partner_api_keys` traegt keine RLS (kein user_id-Bezug, oeffentliche Referenz-/Systemtabelle
 * wie `foods`/`exercises`) -- der Zugriffsschutz liegt komplett in [PartnerApiKeyService]/
 * [PartnerApiKeyFilter], nicht in Postgres. */
@Repository
class PartnerApiKeyRepository(private val jdbcTemplate: JdbcTemplate) {

    fun insert(id: UUID, organizationName: String, contactEmail: String, keyPrefix: String, keyHash: String, at: Instant) {
        jdbcTemplate.update(
            """
            INSERT INTO partner_api_keys (id, organization_name, contact_email, key_prefix, key_hash, created_at)
            VALUES (?, ?, ?, ?, ?, ?)
            """.trimIndent(),
            id, organizationName, contactEmail, keyPrefix, keyHash, Timestamp.from(at),
        )
    }

    /** Nur aktive (nicht widerrufene) Keys authentifizieren. */
    fun findActiveByHash(keyHash: String): PartnerApiKey? =
        jdbcTemplate.query(
            "SELECT * FROM partner_api_keys WHERE key_hash = ? AND revoked_at IS NULL",
            { rs, _ -> rs.toPartnerApiKey() },
            keyHash,
        ).firstOrNull()

    fun touchLastUsed(id: UUID, at: Instant) {
        jdbcTemplate.update("UPDATE partner_api_keys SET last_used_at = ? WHERE id = ?", Timestamp.from(at), id)
    }

    private fun ResultSet.toPartnerApiKey() = PartnerApiKey(
        id = getObject("id", UUID::class.java),
        organizationName = getString("organization_name"),
        contactEmail = getString("contact_email"),
        keyPrefix = getString("key_prefix"),
        revokedAt = getTimestamp("revoked_at")?.toInstant(),
    )
}
