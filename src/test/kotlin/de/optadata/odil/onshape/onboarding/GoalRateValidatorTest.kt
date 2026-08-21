package de.optadata.odil.onshape.onboarding

import kotlin.test.Test
import kotlin.test.assertFailsWith

class GoalRateValidatorTest {

    @Test
    fun `abnehmen im erlaubten Korridor wird akzeptiert`() {
        GoalRateValidator.validate(Goal.LOSE, 0.25)
        GoalRateValidator.validate(Goal.LOSE, 1.0)
        GoalRateValidator.validate(Goal.LOSE, 0.6)
    }

    @Test
    fun `abnehmen ausserhalb des Korridors wird blockiert nicht nur gewarnt`() {
        assertFailsWith<GoalRateExceededException> { GoalRateValidator.validate(Goal.LOSE, 1.5) }
        assertFailsWith<GoalRateExceededException> { GoalRateValidator.validate(Goal.LOSE, 0.1) }
    }

    @Test
    fun `zunehmen und muskelaufbau im erlaubten Korridor werden akzeptiert`() {
        GoalRateValidator.validate(Goal.GAIN_WEIGHT, 0.125)
        GoalRateValidator.validate(Goal.GAIN_WEIGHT, 0.5)
        GoalRateValidator.validate(Goal.GAIN_MUSCLE, 0.3)
    }

    @Test
    fun `zunehmen ausserhalb des Korridors wird blockiert`() {
        assertFailsWith<GoalRateExceededException> { GoalRateValidator.validate(Goal.GAIN_WEIGHT, 0.6) }
        assertFailsWith<GoalRateExceededException> { GoalRateValidator.validate(Goal.GAIN_MUSCLE, 0.05) }
    }

    @Test
    fun `kraft erhaltung und recomp haben keine Ratenbindung`() {
        GoalRateValidator.validate(Goal.STRENGTH, 5.0)
        GoalRateValidator.validate(Goal.MAINTAIN, -3.0)
        GoalRateValidator.validate(Goal.RECOMP, 99.0)
    }
}
