package de.optadata.odil.onshape.auth

import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.stereotype.Repository
import java.sql.ResultSet
import java.util.UUID

/**
 * Datenzugriff auf `users`. Bewusst raw JDBC statt JPA, konsistent mit
 * [de.optadata.odil.onshape.foodimport.FoodImportRepository]. Jeder Aufruf muss innerhalb
 * einer Transaktion laufen, die vorher [de.optadata.odil.onshape.security.RlsSession] den
 * passenden Nutzerkontext gesetzt hat (`asUser` fuer authentifizierte Zugriffe, `asAuthLookup`
 * fuer Registrierung/Login-Lookup vor der Authentifizierung, siehe V9-Migration).
 */
@Repository
class UserRepository(private val jdbcTemplate: JdbcTemplate) {

    fun findByEmail(email: String): User? =
        jdbcTemplate.query(SELECT_SQL + " WHERE email = ?", { rs, _ -> rs.toUser() }, email).firstOrNull()

    fun findById(id: UUID): User? =
        jdbcTemplate.query(SELECT_SQL + " WHERE id = ?", { rs, _ -> rs.toUser() }, id).firstOrNull()

    fun insert(email: String, passwordHash: String?, locale: String): User {
        val id = jdbcTemplate.queryForObject(
            """
            INSERT INTO users (email, password_hash, locale) VALUES (?, ?, ?)
            RETURNING id
            """.trimIndent(),
            UUID::class.java,
            email,
            passwordHash,
            locale,
        ) ?: error("Insert into users returned no id")
        return findById(id) ?: error("Just-inserted user $id not found")
    }

    private fun ResultSet.toUser() = User(
        id = getObject("id", UUID::class.java),
        email = getString("email"),
        passwordHash = getString("password_hash"),
        locale = getString("locale"),
        unitSystem = getString("unit_system"),
        timezone = getString("timezone"),
        createdAt = getTimestamp("created_at").toInstant(),
    )

    private companion object {
        const val SELECT_SQL =
            "SELECT id, email, password_hash, locale, unit_system, timezone, created_at FROM users"
    }
}
