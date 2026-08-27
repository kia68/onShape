package de.optadata.odil.onshape.onboarding

import de.optadata.odil.onshape.billing.SubscriptionService
import de.optadata.odil.onshape.billing.TierPolicy
import de.optadata.odil.onshape.nutrition.FoodEntryRepository
import de.optadata.odil.onshape.progress.AdherenceCalculator
import de.optadata.odil.onshape.progress.DatedValue
import de.optadata.odil.onshape.progress.SevenDayMovingAverage
import de.optadata.odil.onshape.security.RlsSession
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Service
import org.springframework.web.server.ResponseStatusException
import java.time.LocalDate
import java.time.temporal.ChronoUnit
import java.util.UUID

/**
 * FR-134 orchestriert [AdaptiveTdeeCalculator] mit echten Daten gegen `tdee_estimates` (V6,
 * siehe [AdaptiveTdeeRepository]-KDoc). Rollierendes 14-Tage-Fenster, bei jedem Aufruf live
 * berechnet (kein periodischer Job). Der geglaettete Wert wird hoechstens EINMAL pro
 * Kalendertag neu persistiert -- sonst wuerde ein zweiter Aufruf am selben Tag den bereits
 * geblendeten Wert ERNEUT gegen sich selbst blenden (EMA waere nicht mehr "ein Schritt pro
 * Tag"). Ein Puffer VOR dem Fensterbeginn wird mitgeladen, damit das 7-Tage-Mittel am
 * Fensterrand selbst nicht nur aus den ersten, unvollstaendigen Messpunkten besteht.
 *
 * WICHTIG (Scope-Entscheidung): dieser Durchgang schreibt den adaptiven Wert NICHT in
 * `nutrition_targets` zurueck -- das taegliche Kalorienbudget (Tagesansicht, Wochenbericht,
 * Wellbeing-Guardrails) bleibt unveraendert formelbasiert. `tdee_estimates.applied` bleibt daher
 * immer `false`. KONZEPT.md §7.1 beschreibt Adaptives TDEE zwar als Korrektur des tatsaechlichen
 * Ziels, aber das würde quer durch mehrere bestehende Verbraucher (Tagesansicht-Restbudget,
 * FR-135-Wochenbericht-Zielwert, Wellbeing-Schwellenwerte) eingreifen -- ein eigener
 * Integrationsschritt, hier bewusst nur als eigenstaendiger Insight-Endpunkt umgesetzt.
 */
@Service
class AdaptiveTdeeService(
    private val bodyMeasurementRepository: BodyMeasurementRepository,
    private val foodEntryRepository: FoodEntryRepository,
    private val nutritionTargetRepository: NutritionTargetRepository,
    private val adaptiveTdeeRepository: AdaptiveTdeeRepository,
    private val subscriptionService: SubscriptionService,
    private val rlsSession: RlsSession,
) {
    private companion object {
        const val SMOOTHING_BUFFER_DAYS = 30L
    }

    fun compute(userId: UUID, today: LocalDate = LocalDate.now()): AdaptiveTdeeResponse {
        if (!TierPolicy.canShowAdaptiveTdee(subscriptionService.currentTier(userId))) {
            throw AdaptiveTdeeRequiresUpgradeException()
        }

        val windowStart = today.minusDays((AdaptiveTdeeCalculator.MIN_WINDOW_DAYS - 1).toLong())
        val windowDays = (ChronoUnit.DAYS.between(windowStart, today) + 1).toInt()

        val (target, latestEstimate, weightHistory, daily) = rlsSession.asUser(userId) {
            AdaptiveTdeeRawData(
                target = nutritionTargetRepository.findLatest(userId),
                latestEstimate = adaptiveTdeeRepository.findLatest(userId),
                weightHistory = bodyMeasurementRepository.findHistory(userId, windowStart.minusDays(SMOOTHING_BUFFER_DAYS), today),
                daily = foodEntryRepository.findDailyTotals(userId, windowStart, today),
            )
        }
        val formulaTdeeKcal = target?.result?.tdeeKcal
            ?: throw ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY, "Onboarding noch nicht abgeschlossen")
        val weighInsInWindow = weightHistory.count { it.measuredOn >= windowStart && it.weightKg != null }
        val nutritionAdherence = AdherenceCalculator.rate(daily.map { it.date }.toSet(), windowStart, today)

        // Bereits heute berechnet und persistiert -- nicht erneut blenden (siehe Klassen-KDoc).
        if (latestEstimate != null && latestEstimate.computedOn == today) {
            return AdaptiveTdeeResponse(
                eligible = true, reason = null, adaptiveTdeeKcal = latestEstimate.tdeeSmoothedKcal,
                formulaTdeeKcal = formulaTdeeKcal, windowDays = windowDays,
                weighInsInWindow = weighInsInWindow, nutritionAdherence = nutritionAdherence,
            )
        }

        val smoothed = SevenDayMovingAverage.compute(
            weightHistory.filter { it.weightKg != null }.map { DatedValue(it.measuredOn, it.weightKg!!) },
        )
        val smoothedStart = smoothed.filter { it.date <= windowStart }.maxByOrNull { it.date }?.value
        val smoothedEnd = smoothed.filter { it.date <= today }.maxByOrNull { it.date }?.value
        val avgKcalIntake = if (daily.isEmpty()) null else daily.sumOf { it.kcal } / daily.size
        val seedTdeeKcal = latestEstimate?.tdeeSmoothedKcal ?: formulaTdeeKcal

        val input = AdaptiveTdeeInput(
            smoothedWeightStartKg = smoothedStart, smoothedWeightEndKg = smoothedEnd,
            windowDays = windowDays, weighInsInWindow = weighInsInWindow,
            avgKcalIntake = avgKcalIntake, nutritionAdherence = nutritionAdherence, seedTdeeKcal = seedTdeeKcal,
        )
        val result = AdaptiveTdeeCalculator.evaluate(input)

        return when (result) {
            is AdaptiveTdeeResult.Eligible -> {
                rlsSession.asUser(userId) {
                    adaptiveTdeeRepository.insert(
                        userId,
                        TdeeEstimate(
                            computedOn = today, windowDays = windowDays, avgIntakeKcal = avgKcalIntake!!,
                            weightDeltaKg = smoothedEnd!! - smoothedStart!!, tdeeObservedKcal = result.tdeeObservedKcal,
                            tdeeSmoothedKcal = result.tdeeKcal, logAdherence = nutritionAdherence, applied = false,
                        ),
                    )
                }
                AdaptiveTdeeResponse(
                    eligible = true, reason = null, adaptiveTdeeKcal = result.tdeeKcal,
                    formulaTdeeKcal = formulaTdeeKcal, windowDays = windowDays,
                    weighInsInWindow = weighInsInWindow, nutritionAdherence = nutritionAdherence,
                )
            }
            is AdaptiveTdeeResult.Ineligible -> AdaptiveTdeeResponse(
                eligible = false, reason = result.reason, adaptiveTdeeKcal = null,
                formulaTdeeKcal = formulaTdeeKcal, windowDays = windowDays,
                weighInsInWindow = weighInsInWindow, nutritionAdherence = nutritionAdherence,
            )
        }
    }

    private data class AdaptiveTdeeRawData(
        val target: StoredNutritionTarget?,
        val latestEstimate: TdeeEstimate?,
        val weightHistory: List<BodyMeasurement>,
        val daily: List<de.optadata.odil.onshape.nutrition.DailyNutritionTotal>,
    )
}
