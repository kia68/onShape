package de.optadata.odil.onshape.billing

import java.security.MessageDigest
import java.time.Instant
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec
import kotlin.math.abs

/** BIZ-02: verifiziert die `Stripe-Signature`-Header nach Stripes eigenem Verfahren (HMAC-SHA256
 * ueber "timestamp.payload", siehe Stripe-Doku "Verify webhook signatures") -- reine, DB- und
 * netzwerkfreie Funktion, deshalb ohne echten Stripe-Account testbar (NFR-13). Handgerollt statt
 * dem Stripe-SDK, gleiches Muster wie CsvReader/JWT in frueheren Epics: eine einzelne HMAC-
 * Pruefung rechtfertigt keine neue Abhaengigkeit. */
object StripeSignatureVerifier {

    /** Stripes eigener Default-Toleranzwert gegen Replay-Angriffe. */
    const val TOLERANCE_SECONDS = 300L

    fun verify(payload: String, sigHeader: String, secret: String, now: Instant = Instant.now()): Boolean {
        val fields = sigHeader.split(",").mapNotNull {
            val parts = it.split("=", limit = 2)
            if (parts.size == 2) parts[0] to parts[1] else null
        }.toMap()

        val timestamp = fields["t"]?.toLongOrNull() ?: return false
        val signature = fields["v1"] ?: return false
        if (abs(now.epochSecond - timestamp) > TOLERANCE_SECONDS) return false

        val expected = hmacSha256Hex("$timestamp.$payload", secret)
        return MessageDigest.isEqual(expected.toByteArray(Charsets.UTF_8), signature.toByteArray(Charsets.UTF_8))
    }

    private fun hmacSha256Hex(data: String, key: String): String {
        val mac = Mac.getInstance("HmacSHA256")
        mac.init(SecretKeySpec(key.toByteArray(Charsets.UTF_8), "HmacSHA256"))
        return mac.doFinal(data.toByteArray(Charsets.UTF_8)).joinToString("") { "%02x".format(it) }
    }
}
