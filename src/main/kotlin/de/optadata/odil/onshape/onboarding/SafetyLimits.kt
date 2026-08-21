package de.optadata.odil.onshape.onboarding

import java.time.LocalDate
import java.time.Period

class UnderMinimumAgeException(age: Int) :
    RuntimeException("Mindestalter 16 Jahre nicht erreicht (Alter: $age)")

class TargetWeightBmiTooLowException(bmi: Double) :
    RuntimeException("Zielgewicht wuerde zu BMI ${"%.1f".format(bmi)} fuehren (Minimum 18.5)")

/** KONZEPT.md §7.1 "Harte Sicherheitsgrenzen (nicht ueberschreibbar)". */
object SafetyLimits {
    const val MINIMUM_AGE_YEARS = 16
    const val MINIMUM_BMI = 18.5

    fun requireMinimumAge(birthDate: LocalDate, today: LocalDate) {
        val age = Period.between(birthDate, today).years
        if (age < MINIMUM_AGE_YEARS) throw UnderMinimumAgeException(age)
    }

    /** Nur relevant, wenn ein Zielgewicht angegeben ist (FR-02/FR-04 optionales Feld). */
    fun requireHealthyTargetBmi(targetWeightKg: Double?, heightCm: Double) {
        if (targetWeightKg == null) return
        val heightM = heightCm / 100.0
        val bmi = targetWeightKg / (heightM * heightM)
        if (bmi < MINIMUM_BMI) throw TargetWeightBmiTooLowException(bmi)
    }
}
