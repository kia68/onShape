package de.optadata.odil.onshape.security

import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import tools.jackson.databind.ObjectMapper
import java.security.MessageDigest
import java.time.Clock
import java.time.Instant
import java.time.temporal.ChronoUnit
import java.util.Base64
import java.util.UUID
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec

data class JwtClaims(val userId: UUID, val email: String, val expiresAt: Instant)

/**
 * Handgerollter HS256-JWT statt einer zusaetzlichen Bibliothek (jjwt zieht eine alte
 * com.fasterxml.jackson-Version, die App ist auf Jackson 3 / `tools.jackson` (Boot 4.1)
 * umgestellt -- Versionskonflikt vermieden). Nur die zwei Operationen, die die App braucht:
 * signierte Access-Tokens ausstellen und pruefen.
 */
@Component
class JwtService(
    @Value("\${app.jwt.secret}") secret: String,
    @Value("\${app.jwt.ttl-minutes:43200}") private val ttlMinutes: Long,
    private val objectMapper: ObjectMapper,
    private val clock: Clock = Clock.systemUTC(),
) {
    private val log = LoggerFactory.getLogger(JwtService::class.java)
    private val key = SecretKeySpec(secret.toByteArray(Charsets.UTF_8), "HmacSHA256")

    fun issue(userId: UUID, email: String): String {
        val now = Instant.now(clock)
        val header = """{"alg":"HS256","typ":"JWT"}"""
        val payload = objectMapper.writeValueAsString(
            mapOf(
                "sub" to userId.toString(),
                "email" to email,
                "iat" to now.epochSecond,
                "exp" to now.plus(ttlMinutes, ChronoUnit.MINUTES).epochSecond,
            )
        )
        val signingInput = "${header.b64()}.${payload.b64()}"
        return "$signingInput.${sign(signingInput)}"
    }

    fun parse(token: String): JwtClaims? {
        val parts = token.split(".")
        if (parts.size != 3) return null
        val (headerB64, payloadB64, signature) = parts
        val expectedSignature = sign("$headerB64.$payloadB64")
        if (!MessageDigest.isEqual(expectedSignature.toByteArray(Charsets.UTF_8), signature.toByteArray(Charsets.UTF_8))) {
            log.debug("JWT signature mismatch")
            return null
        }
        return runCatching {
            val payload = objectMapper.readValue(payloadB64.deB64(), Map::class.java)
            val expiresAt = Instant.ofEpochSecond((payload["exp"] as Number).toLong())
            if (expiresAt.isBefore(Instant.now(clock))) return null
            JwtClaims(
                userId = UUID.fromString(payload["sub"] as String),
                email = payload["email"] as String,
                expiresAt = expiresAt,
            )
        }.getOrElse {
            log.debug("JWT payload malformed", it)
            null
        }
    }

    private fun sign(input: String): String {
        val mac = Mac.getInstance("HmacSHA256")
        mac.init(key)
        return Base64.getUrlEncoder().withoutPadding().encodeToString(mac.doFinal(input.toByteArray(Charsets.UTF_8)))
    }

    private fun String.b64(): String =
        Base64.getUrlEncoder().withoutPadding().encodeToString(toByteArray(Charsets.UTF_8))

    private fun String.deB64(): String =
        String(Base64.getUrlDecoder().decode(this), Charsets.UTF_8)
}
