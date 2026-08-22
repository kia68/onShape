package de.optadata.odil.onshape.integrations

import java.time.Instant
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class StrongCsvParserTest {

    private val header = "Date,Workout Name,Duration,Exercise Name,Set Order,Weight,Reps,Distance,Seconds,Notes,Workout Notes,RPE"

    @Test
    fun `parst eine reale beispielzeile im original strong-format`() {
        val csv = header + "\n" +
            "2020-12-30 18:51:52,\"Evening Workout\",2h 38m,\"Snatch (Barbell)\",1,40.0,3,0,0,\"\",\"\",\n"

        val result = StrongCsvParser.parse(csv)

        assertEquals(1, result.rows.size)
        val row = result.rows.single()
        assertEquals("Snatch (Barbell)", row.exerciseName)
        assertEquals(40.0, row.weightKg)
        assertEquals(3, row.reps)
        assertEquals(0, row.setIndex)
        assertEquals(Instant.parse("2020-12-30T18:51:52Z"), row.startedAt)
        assertEquals(Instant.parse("2020-12-30T21:29:52Z"), row.finishedAt)
    }

    @Test
    fun `set order ist eins-basiert in der datei und wird auf null-basiert normalisiert`() {
        val csv = header + "\n" +
            "2026-01-01 08:00:00,\"W\",45m,\"Bench Press (Barbell)\",1,60,8,0,0,,,\n" +
            "2026-01-01 08:00:00,\"W\",45m,\"Bench Press (Barbell)\",2,60,7,0,0,,,\n"

        val result = StrongCsvParser.parse(csv)

        assertEquals(0, result.rows[0].setIndex)
        assertEquals(1, result.rows[1].setIndex)
    }

    @Test
    fun `distanzwerte werden nicht importiert aber als warnung gemeldet`() {
        val csv = header + "\n" +
            "2026-01-01 08:00:00,\"Run\",30m,\"Treadmill\",1,0,0,5,0,,,\n"

        val result = StrongCsvParser.parse(csv)

        assertNull(result.rows.single().distanceM)
        assertTrue(result.warnings.any { it.contains("Distanzwert") })
    }

    @Test
    fun `gewicht wird unveraendert uebernommen und mit einheiten-warnung versehen`() {
        val csv = header + "\n" +
            "2026-01-01 08:00:00,\"W\",45m,\"Bench Press (Barbell)\",1,60,8,0,0,,,\n"

        val result = StrongCsvParser.parse(csv)

        assertEquals(60.0, result.rows.single().weightKg)
        assertTrue(result.warnings.any { it.contains("kg") })
    }

    @Test
    fun `zeilen ohne exercise name werden uebersprungen`() {
        val csv = header + "\n" +
            "2026-01-01 08:00:00,\"W\",45m,\"\",1,60,8,0,0,,,\n"

        val result = StrongCsvParser.parse(csv)

        assertTrue(result.rows.isEmpty())
    }
}
