package de.optadata.odil.onshape.billing

import java.time.Instant
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/** BIZ-02: unabhaengige Referenz-Implementierung der HMAC-Berechnung im Test (nicht dieselbe
 * private Funktion aus [StripeSignatureVerifier] wiederverwendet), damit der Test tatsaechlich
 * Stripes Verfahren nachbildet statt nur den eigenen Code gegen sich selbst zu spiegeln. */
class StripeSignatureVerifierTest {

    private val secret = "whsec_test_secret"

    private fun sign(payload: String, timestamp: Long): String {
        val mac = Mac.getInstance("HmacSHA256")
        mac.init(SecretKeySpec(secret.toByteArray(), "HmacSHA256"))
        val hex = mac.doFinal("$timestamp.$payload".toByteArray()).joinToString("") { "%02x".format(it) }
        return "t=$timestamp,v1=$hex"
    }

    @Test
    fun `gueltige signatur wird akzeptiert`() {
        val payload = """{"type":"checkout.session.completed"}"""
        val now = Instant.parse("2026-08-26T12:00:00Z")
        val header = sign(payload, now.epochSecond)

        assertTrue(StripeSignatureVerifier.verify(payload, header, secret, now))
    }

    @Test
    fun `veraenderter payload wird abgelehnt`() {
        val now = Instant.parse("2026-08-26T12:00:00Z")
        val header = sign("""{"type":"checkout.session.completed"}""", now.epochSecond)

        assertFalse(StripeSignatureVerifier.verify("""{"type":"customer.subscription.deleted"}""", header, secret, now))
    }

    @Test
    fun `falsches secret wird abgelehnt`() {
        val payload = """{"type":"checkout.session.completed"}"""
        val now = Instant.parse("2026-08-26T12:00:00Z")
        val header = sign(payload, now.epochSecond)

        assertFalse(StripeSignatureVerifier.verify(payload, header, "wrong-secret", now))
    }

    @Test
    fun `zu alter zeitstempel wird als replay abgelehnt`() {
        val payload = """{"type":"checkout.session.completed"}"""
        val signedAt = Instant.parse("2026-08-26T12:00:00Z")
        val header = sign(payload, signedAt.epochSecond)
        val muchLater = signedAt.plusSeconds(StripeSignatureVerifier.TOLERANCE_SECONDS + 1)

        assertFalse(StripeSignatureVerifier.verify(payload, header, secret, muchLater))
    }

    @Test
    fun `fehlendes v1-feld wird abgelehnt`() {
        assertFalse(StripeSignatureVerifier.verify("{}", "t=1700000000", secret))
    }
}
