package de.optadata.odil.onshape.integrations

import java.time.Instant
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class HevyCsvParserTest {

    private val header = "\"title\",\"start_time\",\"end_time\",\"description\",\"exercise_title\",\"superset_id\",\"exercise_notes\"," +
        "\"set_index\",\"set_type\",\"weight_kg\",\"reps\",\"distance_km\",\"duration_seconds\",\"rpe\""

    @Test
    fun `parst eine reale beispielzeile mit dem hevy-datumsformat`() {
        val csv = header + "\n" +
            "\"Morning workout\",\"22 Dec 2025, 08:00\",\"22 Dec 2025, 08:37\",\"\",\"Pull Up (Assisted)\",,\"\",0,\"normal\",21,10,,0,8.5\n"

        val result = HevyCsvParser.parse(csv)

        assertEquals(1, result.rows.size)
        val row = result.rows.single()
        assertEquals("Pull Up (Assisted)", row.exerciseName)
        assertEquals(21.0, row.weightKg)
        assertEquals(10, row.reps)
        assertEquals(0, row.setIndex)
        assertEquals(Instant.parse("2025-12-22T08:00:00Z"), row.startedAt)
        assertEquals(Instant.parse("2025-12-22T08:37:00Z"), row.finishedAt)
    }

    @Test
    fun `mehrere saetze derselben session teilen sich denselben sessionKey`() {
        val csv = header + "\n" +
            "\"Leg Day\",\"1 Jan 2026, 10:00\",\"1 Jan 2026, 10:45\",\"\",\"Back Squat\",,\"\",0,\"normal\",60,8,,0,7\n" +
            "\"Leg Day\",\"1 Jan 2026, 10:00\",\"1 Jan 2026, 10:45\",\"\",\"Back Squat\",,\"\",1,\"normal\",65,6,,0,8\n"

        val result = HevyCsvParser.parse(csv)

        assertEquals(2, result.rows.size)
        assertEquals(result.rows[0].sessionKey, result.rows[1].sessionKey)
    }

    @Test
    fun `weight_lbs wird nach kg umgerechnet`() {
        val lbsHeader = header.replace("\"weight_kg\"", "\"weight_lbs\"")
        val csv = lbsHeader + "\n" +
            "\"W\",\"1 Jan 2026, 10:00\",\"1 Jan 2026, 10:45\",\"\",\"Bench\",,\"\",0,\"normal\",100,5,,0,8\n"

        val result = HevyCsvParser.parse(csv)

        assertEquals(45.359237, result.rows.single().weightKg!!, 0.0001)
    }

    @Test
    fun `zeilen ohne exercise_title werden uebersprungen und als warnung gemeldet`() {
        val csv = header + "\n" +
            "\"W\",\"1 Jan 2026, 10:00\",\"1 Jan 2026, 10:45\",\"\",\"\",,\"\",0,\"normal\",,,,0,\n"

        val result = HevyCsvParser.parse(csv)

        assertTrue(result.rows.isEmpty())
        assertEquals(1, result.warnings.size)
    }

    @Test
    fun `leeres end_time ergibt kein finishedAt`() {
        val csv = header + "\n" +
            "\"W\",\"1 Jan 2026, 10:00\",\"\",\"\",\"Bench\",,\"\",0,\"normal\",100,5,,0,8\n"

        val result = HevyCsvParser.parse(csv)

        assertNull(result.rows.single().finishedAt)
    }
}
