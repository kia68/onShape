package de.optadata.odil.onshape.partnerapi

import java.security.MessageDigest
import java.security.SecureRandom

/**
 * Reine, DB-freie Kernlogik fuer Partner-API-Keys (NFR-13 testbar), gleiches Muster wie
 * [de.optadata.odil.onshape.billing.StripeSignatureVerifier]. Anders als Nutzerpasswoerter
 * (Argon2id, siehe PasswordEncoderConfig) sind API-Keys selbst schon hochentropische
 * Zufallswerte, kein von Menschen gewaehltes Geheimnis -- ein schneller kryptographischer Hash
 * (SHA-256) reicht dafuer aus, ein absichtlich langsamer Passwort-Hash waere hier nur unnoetiger
 * Overhead pro API-Aufruf.
 */
object PartnerApiKeyHasher {

    private const val PREFIX = "pak_live_"
    private const val RANDOM_BYTES = 24
    private val random = SecureRandom()

    /** @return Klartext-Key im Format `pak_live_<48 Hex-Zeichen>`. */
    fun generate(): String {
        val bytes = ByteArray(RANDOM_BYTES)
        random.nextBytes(bytes)
        return PREFIX + bytes.toHex()
    }

    /** Erste 12 Zeichen (Praefix + 3 Byte), fuer Support/Anzeige ohne den vollen Key zu speichern. */
    fun displayPrefix(plaintextKey: String): String = plaintextKey.take(PREFIX.length + 6)

    fun hash(plaintextKey: String): String =
        MessageDigest.getInstance("SHA-256").digest(plaintextKey.toByteArray(Charsets.UTF_8)).toHex()

    private fun ByteArray.toHex(): String = joinToString("") { "%02x".format(it) }
}
