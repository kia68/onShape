package de.optadata.odil.onshape.trainlog

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class OneRepMaxCalculatorTest {

    @Test
    fun `ein satz mit einer wiederholung liefert das gewicht selbst`() {
        assertEquals(100.0, OneRepMaxCalculator.estimate(100.0, 1))
    }

    @Test
    fun `epley und brzycki werden gemittelt`() {
        // 100kg x 5: Epley = 100*(1+5/30) = 116.667, Brzycki = 100*36/32 = 112.5 -> Mittel 114.583
        val result = OneRepMaxCalculator.estimate(100.0, 5)!!
        assertEquals(114.583, result, 0.01)
    }

    @Test
    fun `hoehere wiederholungszahl bei gleichem gewicht ergibt hoeheres geschaetztes 1rm`() {
        val low = OneRepMaxCalculator.estimate(80.0, 5)!!
        val high = OneRepMaxCalculator.estimate(80.0, 10)!!
        assert(high > low)
    }

    @Test
    fun `ungueltige eingaben liefern null statt fehler`() {
        assertNull(OneRepMaxCalculator.estimate(0.0, 5))
        assertNull(OneRepMaxCalculator.estimate(100.0, 0))
        assertNull(OneRepMaxCalculator.estimate(-10.0, 5))
    }

    @Test
    fun `sehr hohe wiederholungszahlen fuehren nicht zu einem fehler an brzyckis polstelle`() {
        val result = OneRepMaxCalculator.estimate(20.0, 40)
        assert(result != null && result.isFinite())
    }
}
