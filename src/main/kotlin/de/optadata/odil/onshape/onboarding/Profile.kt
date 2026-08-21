package de.optadata.odil.onshape.onboarding

import java.time.LocalDate
import java.util.UUID

/**
 * Spiegelt `profiles` aus V1__extensions_users_profile.sql (FR-02 .. FR-09). Das aktuelle
 * Gewicht gehoert NICHT hierher, sondern in `body_measurements` (V6, kanonische Gewichtshistorie
 * fuer Fortschrittsauswertung) -- das Onboarding legt dort den ersten Messpunkt an, siehe
 * [OnboardingService].
 */
data class Profile(
    val userId: UUID,
    val sex: Sex,
    val birthDate: LocalDate,
    val heightCm: Double,
    val experience: Experience,
    val activityPal: Double,
    val goal: Goal,
    val goalRatePctWeek: Double,
    val targetWeightKg: Double?,
    val bodyFatPct: Double?,
    val dietaryPrefs: List<String>,
    val allergens: List<String>,
    val injuries: List<String>,
    val equipment: List<String>,
    val trainingDaysWeek: Int,
    val sessionMinutes: Int,
)
