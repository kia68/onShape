package de.optadata.odil.onshape.security

import java.time.Clock
import java.time.Duration
import java.time.Instant
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicInteger

/** NFR-08 (Rate-Limiting). Fest-Fenster-Zaehler pro Schluessel (z.B. Client-IP): erlaubt bis zu
 * `maxRequests` Aufrufe pro `windowSeconds`, danach wird abgelehnt bis das Fenster verstreicht.
 * Bewusst kein Bucket4j/keine neue Abhaengigkeit fuer so eine kleine Anforderung -- reine,
 * threadsichere Klasse ohne Servlet-Bezug, damit sie ohne Spring-Kontext testbar ist. */
class RateLimiter(
    private val maxRequests: Int,
    private val windowSeconds: Long,
    private val clock: Clock = Clock.systemUTC(),
) {
    private class Window(val start: Instant, val count: AtomicInteger)

    private val windows = ConcurrentHashMap<String, Window>()

    /** true = erlaubt, false = Limit fuer dieses Fenster erschoepft. */
    fun tryAcquire(key: String): Boolean {
        val now = Instant.now(clock)
        val window = windows.compute(key) { _, existing ->
            if (existing == null || Duration.between(existing.start, now).seconds >= windowSeconds) {
                Window(now, AtomicInteger(0))
            } else {
                existing
            }
        }!!
        return window.count.incrementAndGet() <= maxRequests
    }
}
