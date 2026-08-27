package de.optadata.odil.onshape.onboarding

import kotlin.math.roundToInt

enum class AdaptiveTdeeIneligibleReason { INSUFFICIENT_WINDOW, INSUFFICIENT_WEIGH_INS, INSUFFICIENT_ADHERENCE, MISSING_WEIGHT_DATA }

sealed interface AdaptiveTdeeResult {
    /** [tdeeKcal] ist der geglaettete EMA-Wert (persistiert), [tdeeObservedKcal] die rohe
     * TDEE_real-Schaetzung dieses Laufs (fuer die Audit-Historie in `tdee_estimates`, V6). */
    data class Eligible(val tdeeKcal: Int, val tdeeObservedKcal: Double) : AdaptiveTdeeResult
    data class Ineligible(val reason: AdaptiveTdeeIneligibleReason) : AdaptiveTdeeResult
}

data class AdaptiveTdeeInput(
    /** 7-Tage-geglaettetes Gewicht am Fensterbeginn bzw. -ende (siehe [de.optadata.odil.onshape.progress.SevenDayMovingAverage]) -- null, wenn kein Messpunkt nahe genug am jeweiligen Rand liegt. */
    val smoothedWeightStartKg: Double?,
    val smoothedWeightEndKg: Double?,
    val windowDays: Int,
    val weighInsInWindow: Int,
    /** Durchschnitt NUR ueber tatsaechlich geloggte Tage (nicht Kalendertage) -- "Ø Kalorienzufuhr" bezieht sich auf geloggte Tage. */
    val avgKcalIntake: Double?,
    /** Anteil der Tage im Fenster mit mindestens einem Ernaehrungs-Log-Eintrag, siehe [de.optadata.odil.onshape.progress.AdherenceCalculator]. */
    val nutritionAdherence: Double,
    /** TDEE_adaptiv_alt: der zuletzt gespeicherte adaptive Wert, oder falls noch keiner existiert die formelbasierte TDEE-Schaetzung (Schritt 1-3). */
    val seedTdeeKcal: Int,
)

/**
 * FR-134 (KONZEPT.md §7.1 Schritt 4 "Adaptives TDEE"): voll spezifizierte Formel, im Gegensatz
 * zu den meisten anderen FRs dieser Codebasis keine Prosa-Interpretation noetig -- Zahlen und
 * Formel direkt aus dem Konzeptdokument uebernommen. Reine, DB-freie Kernlogik (NFR-13 testbar).
 *
 * Interpretationsentscheidungen, wo KONZEPT unklar bleibt:
 * - "Adhaerenz beim Logging >= 80%" bezieht sich auf ERNAEHRUNGS-Logging (die Formel braucht
 *   `Ø Kalorienzufuhr`, nur dafuer ist eine Logging-Luecken-Quote sinnvoll) -- die 10-Messpunkte-
 *   Bedingung deckt die Gewichts-Seite separat ab.
 * - "n Tage" (Fenstergroesse) = exakt 14 Tage, das genannte Minimum, nicht ein waechsendes
 *   Fenster -- rollierend neu berechnet bei jedem Aufruf (kein periodischer Job, gleiches Muster
 *   wie FR-133/Volumen-Historie: "Live-Abfrage, keine materialisierte Sicht").
 * - Der Zyklusphasen-Sonderfall (28-Tage-Fenster) ist NICHT umgesetzt -- es gibt in der
 *   gesamten Codebasis keine Zyklus-Erfassung (Profil hat kein entsprechendes Feld), das waere
 *   ein eigenes, hier nicht vorhandenes Feature.
 */
object AdaptiveTdeeCalculator {

    const val MIN_WINDOW_DAYS = 14
    const val MIN_WEIGH_INS = 10
    const val MIN_ADHERENCE = 0.8
    const val KCAL_PER_KG_BODY_FAT = 7700.0

    /** TDEE_adaptiv = 0,7 * TDEE_adaptiv_alt + 0,3 * TDEE_real (KONZEPT.md §7.1 Schritt 4). */
    private const val EMA_WEIGHT_NEW = 0.3

    fun evaluate(input: AdaptiveTdeeInput): AdaptiveTdeeResult {
        if (input.windowDays < MIN_WINDOW_DAYS) return AdaptiveTdeeResult.Ineligible(AdaptiveTdeeIneligibleReason.INSUFFICIENT_WINDOW)
        if (input.weighInsInWindow < MIN_WEIGH_INS) return AdaptiveTdeeResult.Ineligible(AdaptiveTdeeIneligibleReason.INSUFFICIENT_WEIGH_INS)
        if (input.nutritionAdherence < MIN_ADHERENCE) return AdaptiveTdeeResult.Ineligible(AdaptiveTdeeIneligibleReason.INSUFFICIENT_ADHERENCE)
        val start = input.smoothedWeightStartKg ?: return AdaptiveTdeeResult.Ineligible(AdaptiveTdeeIneligibleReason.MISSING_WEIGHT_DATA)
        val end = input.smoothedWeightEndKg ?: return AdaptiveTdeeResult.Ineligible(AdaptiveTdeeIneligibleReason.MISSING_WEIGHT_DATA)
        val avgKcal = input.avgKcalIntake ?: return AdaptiveTdeeResult.Ineligible(AdaptiveTdeeIneligibleReason.MISSING_WEIGHT_DATA)

        val deltaWeightKg = end - start
        val tdeeReal = avgKcal - (deltaWeightKg * KCAL_PER_KG_BODY_FAT / input.windowDays)
        val blended = (1 - EMA_WEIGHT_NEW) * input.seedTdeeKcal + EMA_WEIGHT_NEW * tdeeReal
        return AdaptiveTdeeResult.Eligible(blended.roundToInt(), tdeeReal)
    }
}
