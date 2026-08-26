package de.optadata.odil.onshape.wellbeing

import java.time.Instant

/** Nur der Name der Anlaufstelle, bewusst OHNE Telefonnummer/URL -- KONZEPT.md §14.5 nennt beide
 * Anlaufstellen nur namentlich, eine falsch geratene Kontaktangabe waere bei einem Krisenthema wie
 * gestoertem Essverhalten potenziell schaedlich. Das Frontend verlinkt stattdessen auf die
 * jeweilige offizielle Startseite (siehe die de/en-Uebersetzungsdateien unter messages). */
data class WellbeingResource(val name: String)

val WELLBEING_RESOURCES = listOf(
    WellbeingResource("Bundesfachverband Essstoerungen"),
    WellbeingResource("BZgA-Beratungstelefon"),
)

data class GuardrailStatusResponse(
    val hideCalorieDisplay: Boolean,
    val flags: List<String>,
    val resources: List<WellbeingResource>,
)

data class PauseStatusResponse(val trackingPaused: Boolean, val trackingPausedAt: Instant?)
