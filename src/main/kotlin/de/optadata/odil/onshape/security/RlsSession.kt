package de.optadata.odil.onshape.security

import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional
import java.util.UUID

/**
 * Setzt die von V8__row_level_security.sql / V9__users_auth_lookup_policy.sql erwarteten
 * Postgres-Session-Variablen fuer genau eine Transaktion (`SET LOCAL`, wirkt nur bis
 * COMMIT/ROLLBACK). Die App laeuft mit einem einzigen gepoolten DB-Rollen-Pool -- ohne dieses
 * Pattern wuerden verschiedene Requests auf derselben Connection sich gegenseitig Nutzerkontext
 * "leihen". [asUser] und [asAuthLookup] duerfen deshalb nie in derselben Transaktion kombiniert
 * werden.
 */
@Component
class RlsSession(private val jdbcTemplate: JdbcTemplate) {

    /** Fuer alle Zugriffe eines authentifizierten Requests auf Nutzerdaten (owner_only-Policies). */
    @Transactional
    fun <T> asUser(userId: UUID, block: () -> T): T {
        jdbcTemplate.execute("SET LOCAL app.current_user_id = '$userId'")
        return block()
    }

    /**
     * Ausschliesslich fuer Registrierung und Login-Lookup per E-Mail, bevor ein Nutzerkontext
     * bekannt ist (siehe V9-Kommentar).
     */
    @Transactional
    fun <T> asAuthLookup(block: () -> T): T {
        jdbcTemplate.execute("SET LOCAL app.auth_lookup = 'on'")
        return block()
    }

    /**
     * Ausschliesslich fuer Zugriffe auf `subscriptions` ohne (bzw. quer ueber) Nutzerkontext:
     * Stripe-Webhooks (nur eine Stripe-ID im Event, kein eingeloggter Request) und der
     * Lifetime-Deal-Deckel (muss ALLE Nutzer zaehlen, siehe V17-Kommentar).
     */
    @Transactional
    fun <T> asSystemLookup(block: () -> T): T {
        jdbcTemplate.execute("SET LOCAL app.system_lookup = 'on'")
        return block()
    }
}
