package de.optadata.odil.onshape.progress

import java.time.LocalDate
import kotlin.test.Test
import kotlin.test.assertEquals

class SevenDayMovingAverageTest {

    private fun d(day: Int) = LocalDate.of(2026, 1, day)

    @Test
    fun `erster punkt ist sein eigener durchschnitt`() {
        val result = SevenDayMovingAverage.compute(listOf(DatedValue(d(1), 80.0)))
        assertEquals(80.0, result.single().value)
    }

    @Test
    fun `unter sieben punkten wird ueber alle bisherigen gemittelt`() {
        val points = listOf(DatedValue(d(1), 80.0), DatedValue(d(2), 82.0), DatedValue(d(3), 78.0))
        val result = SevenDayMovingAverage.compute(points)
        assertEquals(80.0, result[0].value)
        assertEquals(81.0, result[1].value)
        assertEquals(80.0, result[2].value, 0.001)
    }

    @Test
    fun `ab dem achten punkt faellt der aelteste aus dem fenster`() {
        // 8 Punkte mit demselben Wert 80, ausser dem ersten (100) -- der muss beim 8. Punkt
        // (Fenstergroesse 7, Index 0 fliegt raus) nicht mehr im Mittel auftauchen.
        val points = (1..8).map { i -> DatedValue(d(i), if (i == 1) 100.0 else 80.0) }
        val result = SevenDayMovingAverage.compute(points)
        assertEquals(80.0, result.last().value, 0.001)
    }

    @Test
    fun `punkte werden unabhaengig von der eingabereihenfolge chronologisch sortiert`() {
        val points = listOf(DatedValue(d(2), 82.0), DatedValue(d(1), 80.0))
        val result = SevenDayMovingAverage.compute(points)
        assertEquals(d(1), result[0].date)
        assertEquals(d(2), result[1].date)
    }
}
