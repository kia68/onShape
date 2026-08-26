package de.optadata.odil.onshape.wellbeing

import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class WellbeingPatternDetectorTest {

    @Test
    fun `drei tage unter 50 prozent des ziels loesen extreme low intake aus`() {
        val flags = WellbeingPatternDetector.evaluate(
            WellbeingInput(
                loggedDailyKcal = listOf(900.0, 950.0, 2000.0, 2100.0, 800.0),
                targetKcal = 2000,
                kcalHistoryChronological = emptyList(),
                trainingSessionsLast7Days = 0,
            ),
        )
        assertEquals(setOf(WellbeingFlag.EXTREME_LOW_INTAKE), flags)
    }

    @Test
    fun `nur zwei niedrige tage loesen extreme low intake nicht aus`() {
        val flags = WellbeingPatternDetector.evaluate(
            WellbeingInput(
                loggedDailyKcal = listOf(900.0, 950.0, 2000.0, 2100.0),
                targetKcal = 2000,
                kcalHistoryChronological = emptyList(),
                trainingSessionsLast7Days = 0,
            ),
        )
        assertTrue(flags.isEmpty())
    }

    @Test
    fun `fehlendes zielkcal ueberspringt die extreme-low-intake-pruefung ohne fehler`() {
        val flags = WellbeingPatternDetector.evaluate(
            WellbeingInput(
                loggedDailyKcal = listOf(10.0, 20.0, 30.0),
                targetKcal = null,
                kcalHistoryChronological = emptyList(),
                trainingSessionsLast7Days = 0,
            ),
        )
        assertTrue(flags.isEmpty())
    }

    @Test
    fun `drei absenkungen des kcal-ziels loesen frequent goal lowering aus`() {
        val flags = WellbeingPatternDetector.evaluate(
            WellbeingInput(
                loggedDailyKcal = emptyList(),
                targetKcal = null,
                kcalHistoryChronological = listOf(2200, 2100, 2000, 1900, 2050),
                trainingSessionsLast7Days = 0,
            ),
        )
        assertEquals(setOf(WellbeingFlag.FREQUENT_GOAL_LOWERING), flags)
    }

    @Test
    fun `nur zwei absenkungen loesen frequent goal lowering nicht aus`() {
        val flags = WellbeingPatternDetector.evaluate(
            WellbeingInput(
                loggedDailyKcal = emptyList(),
                targetKcal = null,
                kcalHistoryChronological = listOf(2200, 2100, 2000, 2050),
                trainingSessionsLast7Days = 0,
            ),
        )
        assertTrue(flags.isEmpty())
    }

    @Test
    fun `steigende ziele zaehlen nicht als absenkung`() {
        val flags = WellbeingPatternDetector.evaluate(
            WellbeingInput(
                loggedDailyKcal = emptyList(),
                targetKcal = null,
                kcalHistoryChronological = listOf(1800, 1900, 2000, 2100, 2200),
                trainingSessionsLast7Days = 0,
            ),
        )
        assertTrue(flags.isEmpty())
    }

    @Test
    fun `sieben trainingstage in folge loesen excessive training aus`() {
        val flags = WellbeingPatternDetector.evaluate(
            WellbeingInput(
                loggedDailyKcal = emptyList(),
                targetKcal = null,
                kcalHistoryChronological = emptyList(),
                trainingSessionsLast7Days = 7,
            ),
        )
        assertEquals(setOf(WellbeingFlag.EXCESSIVE_TRAINING), flags)
    }

    @Test
    fun `sechs trainingstage loesen excessive training nicht aus`() {
        val flags = WellbeingPatternDetector.evaluate(
            WellbeingInput(
                loggedDailyKcal = emptyList(),
                targetKcal = null,
                kcalHistoryChronological = emptyList(),
                trainingSessionsLast7Days = 6,
            ),
        )
        assertTrue(flags.isEmpty())
    }

    @Test
    fun `mehrere muster koennen gleichzeitig ausgeloest werden`() {
        val flags = WellbeingPatternDetector.evaluate(
            WellbeingInput(
                loggedDailyKcal = listOf(500.0, 600.0, 700.0),
                targetKcal = 2000,
                kcalHistoryChronological = listOf(2200, 2100, 2000, 1900),
                trainingSessionsLast7Days = 7,
            ),
        )
        assertEquals(setOf(WellbeingFlag.EXTREME_LOW_INTAKE, WellbeingFlag.FREQUENT_GOAL_LOWERING, WellbeingFlag.EXCESSIVE_TRAINING), flags)
    }
}
