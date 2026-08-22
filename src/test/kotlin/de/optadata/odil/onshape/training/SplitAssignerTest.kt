package de.optadata.odil.onshape.training

import de.optadata.odil.onshape.onboarding.Experience
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class SplitAssignerTest {

    @Test
    fun `anfaenger bekommen immer ganzkoerper unabhaengig von der tageszahl`() {
        for (days in 2..6) {
            val plan = SplitAssigner.assign(days, Experience.BEGINNER)
            assertEquals("full_body", plan.splitType)
            assertEquals(days, plan.days.size)
            assertTrue(plan.days.all { it.patterns.containsAll(BASE_MOVEMENT_PATTERNS) })
        }
    }

    @Test
    fun `noch nie trainiert zaehlt auch als anfaenger`() {
        val plan = SplitAssigner.assign(5, Experience.NONE)
        assertEquals("full_body", plan.splitType)
    }

    @Test
    fun `vier tage fortgeschritten ergibt oberkoerper unterkoerper`() {
        val plan = SplitAssigner.assign(4, Experience.INTERMEDIATE)
        assertEquals("upper_lower", plan.splitType)
        assertEquals(listOf("upper", "lower", "upper", "lower"), plan.days.map { it.nameKey })
    }

    @Test
    fun `fuenf tage ergibt push pull legs plus upper lower`() {
        val plan = SplitAssigner.assign(5, Experience.INTERMEDIATE)
        assertEquals("ppl_upper_lower", plan.splitType)
        assertEquals(listOf("push", "pull", "legs", "upper", "lower"), plan.days.map { it.nameKey })
    }

    @Test
    fun `sechs tage ergibt push pull legs zweimal`() {
        val plan = SplitAssigner.assign(6, Experience.ADVANCED)
        assertEquals("ppl", plan.splitType)
        assertEquals(listOf("push", "pull", "legs", "push", "pull", "legs"), plan.days.map { it.nameKey })
    }

    @Test
    fun `forceFullBody ueberschreibt auch fortgeschrittene`() {
        val plan = SplitAssigner.assign(6, Experience.ADVANCED, forceFullBody = true)
        assertEquals("full_body", plan.splitType)
        assertEquals(6, plan.days.size)
    }

    @Test
    fun `alle sechs grundmuster sind ueber die woche abgedeckt bei jedem split`() {
        for (days in 2..6) {
            for (experience in Experience.entries) {
                val plan = SplitAssigner.assign(days, experience)
                val coveredPatterns = plan.days.flatMap { it.patterns }.toSet()
                assertTrue(
                    coveredPatterns.containsAll(BASE_MOVEMENT_PATTERNS),
                    "Split $days/$experience deckt nicht alle Grundmuster ab: $coveredPatterns",
                )
            }
        }
    }
}
