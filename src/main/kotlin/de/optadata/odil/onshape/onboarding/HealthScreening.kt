package de.optadata.odil.onshape.onboarding

/** FR-07: PAR-Q+-Kurzform. Antworten fuehren NIE zum Ausschluss, nur zu einem Hinweis. */
data class HealthScreeningAnswers(
    val heartCondition: Boolean,
    val pregnancy: Boolean,
    val recentInjury: Boolean,
    val medication: Boolean,
)

data class HealthScreeningResult(val needsMedicalAdvice: Boolean, val triggeredFlags: List<String>)

object HealthScreening {
    fun evaluate(answers: HealthScreeningAnswers): HealthScreeningResult {
        val flags = buildList {
            if (answers.heartCondition) add("heart_condition")
            if (answers.pregnancy) add("pregnancy")
            if (answers.recentInjury) add("recent_injury")
            if (answers.medication) add("medication")
        }
        return HealthScreeningResult(needsMedicalAdvice = flags.isNotEmpty(), triggeredFlags = flags)
    }
}
