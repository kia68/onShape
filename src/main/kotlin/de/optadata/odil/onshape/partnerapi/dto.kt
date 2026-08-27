package de.optadata.odil.onshape.partnerapi

import jakarta.validation.constraints.Email
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size

data class RegisterPartnerRequest(
    @field:NotBlank @field:Size(max = 200) val organizationName: String,
    @field:NotBlank @field:Email val contactEmail: String,
)

data class RegisterPartnerResponse(
    val apiKey: String,
    val keyPrefix: String,
    val note: String = "Dieser Key wird nur einmal angezeigt und nicht im Klartext gespeichert -- sicher aufbewahren.",
)

/** Nur produktbezogene Katalogdaten (Naehrwerte), bewusst OHNE personalisierten Fit-Score
 * (KONZEPT.md §14.1 Zweckbindung: Gesundheitsdaten gehen nie an Dritte -- der Fit-Score braucht
 * ein Nutzerprofil, hier gibt es keinen eingeloggten Nutzer). */
data class PartnerFoodResponse(
    val barcode: String?,
    val brand: String?,
    val name: String,
    val category: String?,
    val novaGroup: Int?,
    val nutriscore: Char?,
    val kcalPer100g: Double,
    val proteinGPer100g: Double,
    val fatGPer100g: Double,
    val carbsGPer100g: Double,
    val sugarGPer100g: Double?,
    val fiberGPer100g: Double?,
    val allergens: List<String>,
)

data class PartnerExerciseSummary(
    val id: String,
    val slug: String,
    val name: String,
    val pattern: String,
    val equipment: List<String>,
    val difficulty: String,
)

data class PartnerExerciseDetailResponse(
    val id: String,
    val slug: String,
    val name: String,
    val pattern: String,
    val equipment: List<String>,
    val difficulty: String,
    val primaryMuscles: List<String>,
    val setupSteps: List<String>,
    val executionSteps: List<String>,
    val cues: List<String>,
    val commonMistakes: List<PartnerExerciseMistake>,
)

data class PartnerExerciseMistake(val title: String, val whyBad: String, val fix: String)
