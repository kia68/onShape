package de.optadata.odil.onshape.onboarding

class GoalRateExceededException(goal: Goal, ratePctWeek: Double, allowed: ClosedFloatingPointRange<Double>) :
    RuntimeException(
        "Zielrate ${"%.3f".format(ratePctWeek)} % KG/Woche fuer $goal ausserhalb erlaubtem Bereich " +
            "${allowed.start}-${allowed.endInclusive} % KG/Woche",
    )

/**
 * FR-04: Zielrate mit medizinischen Grenzen -- schnellere Raten werden blockiert, nicht nur
 * gewarnt. Grenzen aus KONZEPT.md §5.1: Abnehmen 0,25-1,0 %, Zunehmen 0,125-0,5 % KG/Woche.
 * Muskelaufbau wird konservativ dem Zunehmen-Bereich zugeordnet (schnellerer Aufbau bedeutet
 * ueberwiegend Fettzunahme, nicht Muskelmasse) -- Annahme, da KONZEPT.md hierfuer keinen
 * eigenen Wert nennt. Kraft/Erhaltung/Recomp haben keine Ratenbindung.
 */
object GoalRateValidator {
    private val LOSE_RANGE = 0.25..1.0
    private val GAIN_RANGE = 0.125..0.5

    fun rangeFor(goal: Goal): ClosedFloatingPointRange<Double>? = when (goal) {
        Goal.LOSE -> LOSE_RANGE
        Goal.GAIN_MUSCLE, Goal.GAIN_WEIGHT -> GAIN_RANGE
        Goal.STRENGTH, Goal.MAINTAIN, Goal.RECOMP -> null
    }

    fun validate(goal: Goal, ratePctWeek: Double) {
        val range = rangeFor(goal) ?: return
        if (ratePctWeek !in range) throw GoalRateExceededException(goal, ratePctWeek, range)
    }
}
