package de.optadata.odil.onshape.security

import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class RateLimiterTest {

    private class MutableClock(var instant: Instant) : Clock() {
        override fun instant() = instant
        override fun withZone(zone: java.time.ZoneId?) = this
        override fun getZone() = ZoneOffset.UTC
    }

    @Test
    fun `erlaubt bis genau zum limit, dann abgelehnt`() {
        val limiter = RateLimiter(maxRequests = 3, windowSeconds = 60)
        assertTrue(limiter.tryAcquire("1.2.3.4"))
        assertTrue(limiter.tryAcquire("1.2.3.4"))
        assertTrue(limiter.tryAcquire("1.2.3.4"))
        assertFalse(limiter.tryAcquire("1.2.3.4"))
    }

    @Test
    fun `unterschiedliche schluessel haben getrennte kontingente`() {
        val limiter = RateLimiter(maxRequests = 1, windowSeconds = 60)
        assertTrue(limiter.tryAcquire("a"))
        assertTrue(limiter.tryAcquire("b"))
        assertFalse(limiter.tryAcquire("a"))
    }

    @Test
    fun `nach ablauf des fensters wird das kontingent zurueckgesetzt`() {
        val clock = MutableClock(Instant.parse("2026-01-01T00:00:00Z"))
        val limiter = RateLimiter(maxRequests = 1, windowSeconds = 60, clock = clock)
        assertTrue(limiter.tryAcquire("x"))
        assertFalse(limiter.tryAcquire("x"))

        clock.instant = clock.instant.plusSeconds(61)
        assertTrue(limiter.tryAcquire("x"))
    }
}
