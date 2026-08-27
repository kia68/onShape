package de.optadata.odil.onshape.training

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class DeloadRecommendationDetectorTest {

    private fun input(
        stagnantExerciseCount: Int = 0,
        rirMisses: Int = 0,
        recentPerceivedEfforts: List<Int> = emptyList(),
        weeksInCalorieDeficit: Int = 0,
    ) = DeloadInput(stagnantExerciseCount, rirMisses, recentPerceivedEfforts, weeksInCalorieDeficit)

    @Test
    fun `keine signale -- keine empfehlung`() {
        assertEquals(emptySet(), DeloadRecommendationDetector.evaluate(input()))
    }

    @Test
    fun `eine stagnierende uebung reicht bereits`() {
        val reasons = DeloadRecommendationDetector.evaluate(input(stagnantExerciseCount = 1))
        assertEquals(setOf(DeloadReason.STAGNANT_PERFORMANCE), reasons)
    }

    @Test
    fun `unter drei rir-verfehlungen loest nichts aus`() {
        val reasons = DeloadRecommendationDetector.evaluate(input(rirMisses = 2))
        assertTrue(reasons.isEmpty())
    }

    @Test
    fun `genau drei rir-verfehlungen loest aus`() {
        val reasons = DeloadRecommendationDetector.evaluate(input(rirMisses = 3))
        assertEquals(setOf(DeloadReason.MISSED_RIR_TARGET), reasons)
    }

    @Test
    fun `hohe erschoepfung braucht mindestens drei sessions`() {
        val reasons = DeloadRecommendationDetector.evaluate(input(recentPerceivedEfforts = listOf(10, 10)))
        assertTrue(reasons.isEmpty())
    }

    @Test
    fun `durchschnitt ab neun ueber drei sessions loest aus`() {
        val reasons = DeloadRecommendationDetector.evaluate(input(recentPerceivedEfforts = listOf(9, 9, 9)))
        assertEquals(setOf(DeloadReason.HIGH_EXHAUSTION), reasons)
    }

    @Test
    fun `durchschnitt unter neun loest nicht aus`() {
        val reasons = DeloadRecommendationDetector.evaluate(input(recentPerceivedEfforts = listOf(9, 8, 8)))
        assertTrue(reasons.isEmpty())
    }

    @Test
    fun `nur die drei juengsten sessions zaehlen, aeltere werden ignoriert`() {
        // Erste drei (neueste) sind hoch genug, die vierte (niedrige) darf den Schnitt nicht senken.
        val reasons = DeloadRecommendationDetector.evaluate(input(recentPerceivedEfforts = listOf(9, 9, 9, 1)))
        assertEquals(setOf(DeloadReason.HIGH_EXHAUSTION), reasons)
    }

    @Test
    fun `acht wochen im defizit loest noch nichts aus`() {
        val reasons = DeloadRecommendationDetector.evaluate(input(weeksInCalorieDeficit = 8))
        assertTrue(reasons.isEmpty())
    }

    @Test
    fun `neun wochen im defizit loest aus`() {
        val reasons = DeloadRecommendationDetector.evaluate(input(weeksInCalorieDeficit = 9))
        assertEquals(setOf(DeloadReason.PROLONGED_CALORIE_DEFICIT), reasons)
    }

    @Test
    fun `mehrere signale gleichzeitig werden alle gemeldet`() {
        val reasons = DeloadRecommendationDetector.evaluate(
            input(stagnantExerciseCount = 2, rirMisses = 5, recentPerceivedEfforts = listOf(10, 10, 10), weeksInCalorieDeficit = 10),
        )
        assertEquals(
            setOf(DeloadReason.STAGNANT_PERFORMANCE, DeloadReason.MISSED_RIR_TARGET, DeloadReason.HIGH_EXHAUSTION, DeloadReason.PROLONGED_CALORIE_DEFICIT),
            reasons,
        )
    }
}
