package de.optadata.odil.onshape.billing

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/** BIZ-01 (KONZEPT.md §15.1): reine Referenzwerte fuer die Tier-Matrix, siehe TierPolicy-KDoc
 * fuer die jeweilige Interpretationsentscheidung. */
class TierPolicyTest {

    @Test
    fun `fit-score scan limit gilt nur fuer free`() {
        assertEquals(10, TierPolicy.fitScoreScanLimitPerMonth(Tier.FREE))
        assertEquals(null, TierPolicy.fitScoreScanLimitPerMonth(Tier.PLUS))
        assertEquals(null, TierPolicy.fitScoreScanLimitPerMonth(Tier.COACH))
    }

    @Test
    fun `fit-score bleibt sichtbar bis genau zum limit, danach gegated`() {
        assertTrue(TierPolicy.canShowFitScore(Tier.FREE, alreadyScoredThisMonth = 9))
        assertFalse(TierPolicy.canShowFitScore(Tier.FREE, alreadyScoredThisMonth = 10))
        assertFalse(TierPolicy.canShowFitScore(Tier.FREE, alreadyScoredThisMonth = 11))
        assertTrue(TierPolicy.canShowFitScore(Tier.PLUS, alreadyScoredThisMonth = 500))
        assertTrue(TierPolicy.canShowFitScore(Tier.COACH, alreadyScoredThisMonth = 500))
    }

    @Test
    fun `free-tier darf genau ein programm erstellen`() {
        assertTrue(TierPolicy.canCreateProgram(Tier.FREE, alreadyCreated = 0))
        assertFalse(TierPolicy.canCreateProgram(Tier.FREE, alreadyCreated = 1))
        assertFalse(TierPolicy.canCreateProgram(Tier.FREE, alreadyCreated = 5))
        assertTrue(TierPolicy.canCreateProgram(Tier.PLUS, alreadyCreated = 100))
        assertTrue(TierPolicy.canCreateProgram(Tier.COACH, alreadyCreated = 100))
    }

    @Test
    fun `volumen-historie ist im free-tier auf 4 wochen begrenzt, sonst unbegrenzt`() {
        assertEquals(4, TierPolicy.volumeHistoryWindowWeeks(Tier.FREE))
        assertEquals(null, TierPolicy.volumeHistoryWindowWeeks(Tier.PLUS))
        assertEquals(null, TierPolicy.volumeHistoryWindowWeeks(Tier.COACH))
    }

    @Test
    fun `mikronaehrstoffe werden im free-tier auf die basis-fuenf gefiltert, sonst unveraendert`() {
        val full = mapOf("iron_mg" to 5.0, "calcium_mg" to 200.0, "vitamin_d_ug" to 2.0, "vitamin_b12_ug" to 1.0, "magnesium_mg" to 50.0, "zinc_mg" to 3.0, "potassium_mg" to 400.0)

        val filtered = TierPolicy.filterMicros(Tier.FREE, full)
        assertEquals(5, filtered.size)
        assertFalse(filtered.containsKey("zinc_mg"))
        assertFalse(filtered.containsKey("potassium_mg"))

        assertEquals(full, TierPolicy.filterMicros(Tier.PLUS, full))
        assertEquals(full, TierPolicy.filterMicros(Tier.COACH, full))
    }

    @Test
    fun `wochenbericht ist komplett gesperrt im free-tier, sonst frei`() {
        assertFalse(TierPolicy.canShowWeeklyReport(Tier.FREE))
        assertTrue(TierPolicy.canShowWeeklyReport(Tier.PLUS))
        assertTrue(TierPolicy.canShowWeeklyReport(Tier.COACH))
    }

    @Test
    fun `adaptives tdee ist komplett gesperrt im free-tier, sonst frei`() {
        assertFalse(TierPolicy.canShowAdaptiveTdee(Tier.FREE))
        assertTrue(TierPolicy.canShowAdaptiveTdee(Tier.PLUS))
        assertTrue(TierPolicy.canShowAdaptiveTdee(Tier.COACH))
    }
}
