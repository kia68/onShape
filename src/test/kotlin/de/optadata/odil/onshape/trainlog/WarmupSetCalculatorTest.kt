package de.optadata.odil.onshape.trainlog

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class WarmupSetCalculatorTest {

    @Test
    fun `kein arbeitsgewicht liefert keine aufwaermsaetze`() {
        assertTrue(WarmupSetCalculator.calculate(null).isEmpty())
    }

    @Test
    fun `sehr leichtes arbeitsgewicht unter der leeren stange liefert keine aufwaermsaetze`() {
        assertTrue(WarmupSetCalculator.calculate(15.0).isEmpty())
    }

    @Test
    fun `genau die leere stange liefert bereits aufwaermsaetze`() {
        assertTrue(WarmupSetCalculator.calculate(20.0).isNotEmpty())
    }

    @Test
    fun `mittleres arbeitsgewicht liefert zwei aufwaermsaetze mit steigender last und sinkenden wiederholungen`() {
        val sets = WarmupSetCalculator.calculate(40.0)
        assertEquals(2, sets.size)
        assertEquals(20.0, sets[0].weightKg)
        assertEquals(5, sets[0].reps)
        assertEquals(30.0, sets[1].weightKg)
        assertEquals(3, sets[1].reps)
        assertTrue(sets[1].weightKg > sets[0].weightKg)
        assertTrue(sets[1].reps < sets[0].reps)
    }

    @Test
    fun `schweres arbeitsgewicht liefert drei aufwaermsaetze`() {
        val sets = WarmupSetCalculator.calculate(100.0)
        assertEquals(3, sets.size)
        assertEquals(40.0, sets[0].weightKg)
        assertEquals(8, sets[0].reps)
        assertEquals(60.0, sets[1].weightKg)
        assertEquals(5, sets[1].reps)
        assertEquals(80.0, sets[2].weightKg)
        assertEquals(3, sets[2].reps)
    }

    @Test
    fun `grenzwert fuer drei saetze ist inklusiv`() {
        assertEquals(3, WarmupSetCalculator.calculate(60.0).size)
        assertEquals(2, WarmupSetCalculator.calculate(59.99).size)
    }

    @Test
    fun `gewichte werden auf zweikommafuenf kilogramm gerundet`() {
        val sets = WarmupSetCalculator.calculate(43.0)
        for (set in sets) {
            assertEquals(0.0, set.weightKg % WarmupSetCalculator.WEIGHT_ROUNDING_KG, 0.0001)
        }
    }

    @Test
    fun `kein aufwaermsatz wird auf null oder darunter gerundet`() {
        val sets = WarmupSetCalculator.calculate(20.0)
        for (set in sets) {
            assertTrue(set.weightKg >= WarmupSetCalculator.WEIGHT_ROUNDING_KG)
        }
    }
}
