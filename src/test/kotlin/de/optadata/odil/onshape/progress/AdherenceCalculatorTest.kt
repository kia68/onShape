package de.optadata.odil.onshape.progress

import java.time.LocalDate
import kotlin.test.Test
import kotlin.test.assertEquals

class AdherenceCalculatorTest {

    private fun d(day: Int) = LocalDate.of(2026, 1, day)

    @Test
    fun `jeden tag geloggt ergibt volle adhaerenz`() {
        val logged = (1..7).map { d(it) }.toSet()
        assertEquals(1.0, AdherenceCalculator.rate(logged, d(1), d(7)))
    }

    @Test
    fun `keinen einzigen tag geloggt ergibt null adhaerenz`() {
        assertEquals(0.0, AdherenceCalculator.rate(emptySet(), d(1), d(7)))
    }

    @Test
    fun `teilweise geloggt ergibt den anteil geloggter tage`() {
        val logged = setOf(d(1), d(3), d(5))
        assertEquals(3.0 / 7.0, AdherenceCalculator.rate(logged, d(1), d(7)), 0.0001)
    }

    @Test
    fun `datumsangaben ausserhalb des zeitraums zaehlen nicht mit`() {
        val logged = setOf(d(1), d(2), d(10))
        assertEquals(2.0 / 7.0, AdherenceCalculator.rate(logged, d(1), d(7)), 0.0001)
    }
}
