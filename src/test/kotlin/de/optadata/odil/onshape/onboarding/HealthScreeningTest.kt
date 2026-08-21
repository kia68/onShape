package de.optadata.odil.onshape.onboarding

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class HealthScreeningTest {

    @Test
    fun `keine Treffer bedeuten keinen Hinweis`() {
        val result = HealthScreening.evaluate(HealthScreeningAnswers(false, false, false, false))
        assertFalse(result.needsMedicalAdvice)
        assertTrue(result.triggeredFlags.isEmpty())
    }

    @Test
    fun `ein Treffer fuehrt zu Hinweis aber nie zu Ausschluss`() {
        val result = HealthScreening.evaluate(HealthScreeningAnswers(heartCondition = true, pregnancy = false, recentInjury = false, medication = false))
        assertTrue(result.needsMedicalAdvice)
        assertEquals(listOf("heart_condition"), result.triggeredFlags)
    }

    @Test
    fun `alle Treffer werden gesammelt`() {
        val result = HealthScreening.evaluate(HealthScreeningAnswers(true, true, true, true))
        assertEquals(listOf("heart_condition", "pregnancy", "recent_injury", "medication"), result.triggeredFlags)
    }
}
