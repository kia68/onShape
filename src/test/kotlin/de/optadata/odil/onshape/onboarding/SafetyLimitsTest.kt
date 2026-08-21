package de.optadata.odil.onshape.onboarding

import java.time.LocalDate
import kotlin.test.Test
import kotlin.test.assertFailsWith

class SafetyLimitsTest {

    private val today = LocalDate.of(2026, 1, 1)

    @Test
    fun `mindestalter 16 wird akzeptiert`() {
        SafetyLimits.requireMinimumAge(LocalDate.of(2010, 1, 1), today)
    }

    @Test
    fun `unter 16 wird blockiert`() {
        assertFailsWith<UnderMinimumAgeException> {
            SafetyLimits.requireMinimumAge(LocalDate.of(2011, 1, 2), today)
        }
    }

    @Test
    fun `zielgewicht ohne bmi-unterschreitung wird akzeptiert`() {
        SafetyLimits.requireHealthyTargetBmi(70.0, 180.0) // BMI 21.6
    }

    @Test
    fun `zielgewicht unter bmi 18,5 wird blockiert`() {
        assertFailsWith<TargetWeightBmiTooLowException> {
            SafetyLimits.requireHealthyTargetBmi(55.0, 180.0) // BMI 16.98
        }
    }

    @Test
    fun `fehlendes zielgewicht wird nicht geprueft`() {
        SafetyLimits.requireHealthyTargetBmi(null, 180.0)
    }
}
