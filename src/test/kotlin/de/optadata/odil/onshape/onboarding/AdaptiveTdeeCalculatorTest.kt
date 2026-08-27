package de.optadata.odil.onshape.onboarding

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

class AdaptiveTdeeCalculatorTest {

    private fun input(
        smoothedWeightStartKg: Double? = 80.0,
        smoothedWeightEndKg: Double? = 79.0,
        windowDays: Int = 14,
        weighInsInWindow: Int = 10,
        avgKcalIntake: Double? = 2000.0,
        nutritionAdherence: Double = 0.9,
        seedTdeeKcal: Int = 2400,
    ) = AdaptiveTdeeInput(smoothedWeightStartKg, smoothedWeightEndKg, windowDays, weighInsInWindow, avgKcalIntake, nutritionAdherence, seedTdeeKcal)

    @Test
    fun `gewichtsverlust erhoeht den realen tdee ueber die zufuhr hinaus`() {
        // delta = -1kg ueber 14 Tage -> TDEE_real = 2000 - (-1 * 7700/14) = 2550
        // blended = 0.7*2400 + 0.3*2550 = 2445
        val result = AdaptiveTdeeCalculator.evaluate(input(smoothedWeightStartKg = 80.0, smoothedWeightEndKg = 79.0, avgKcalIntake = 2000.0, seedTdeeKcal = 2400))
        assertIs<AdaptiveTdeeResult.Eligible>(result)
        assertEquals(2445, result.tdeeKcal)
    }

    @Test
    fun `gewichtszunahme senkt den realen tdee unter die zufuhr -- symmetrisch zur formel`() {
        // delta = +1kg -> TDEE_real = 2000 - (1 * 7700/14) = 1450
        // blended = 0.7*2400 + 0.3*1450 = 2115
        val result = AdaptiveTdeeCalculator.evaluate(input(smoothedWeightStartKg = 79.0, smoothedWeightEndKg = 80.0, avgKcalIntake = 2000.0, seedTdeeKcal = 2400))
        assertIs<AdaptiveTdeeResult.Eligible>(result)
        assertEquals(2115, result.tdeeKcal)
    }

    @Test
    fun `stabiles gewicht -- tdee-real entspricht exakt der zufuhr`() {
        val result = AdaptiveTdeeCalculator.evaluate(input(smoothedWeightStartKg = 80.0, smoothedWeightEndKg = 80.0, avgKcalIntake = 2200.0, seedTdeeKcal = 2200))
        assertIs<AdaptiveTdeeResult.Eligible>(result)
        assertEquals(2200, result.tdeeKcal)
    }

    @Test
    fun `unter 14 tage fenster ist nicht anwendbar`() {
        val result = AdaptiveTdeeCalculator.evaluate(input(windowDays = 13))
        assertEquals(AdaptiveTdeeResult.Ineligible(AdaptiveTdeeIneligibleReason.INSUFFICIENT_WINDOW), result)
    }

    @Test
    fun `genau 14 tage fenster ist anwendbar`() {
        val result = AdaptiveTdeeCalculator.evaluate(input(windowDays = 14))
        assertIs<AdaptiveTdeeResult.Eligible>(result)
    }

    @Test
    fun `unter zehn gewichtsmessungen ist nicht anwendbar`() {
        val result = AdaptiveTdeeCalculator.evaluate(input(weighInsInWindow = 9))
        assertEquals(AdaptiveTdeeResult.Ineligible(AdaptiveTdeeIneligibleReason.INSUFFICIENT_WEIGH_INS), result)
    }

    @Test
    fun `genau zehn gewichtsmessungen ist anwendbar`() {
        val result = AdaptiveTdeeCalculator.evaluate(input(weighInsInWindow = 10))
        assertIs<AdaptiveTdeeResult.Eligible>(result)
    }

    @Test
    fun `unter achtzig prozent ernaehrungs-adhaerenz ist nicht anwendbar`() {
        val result = AdaptiveTdeeCalculator.evaluate(input(nutritionAdherence = 0.79))
        assertEquals(AdaptiveTdeeResult.Ineligible(AdaptiveTdeeIneligibleReason.INSUFFICIENT_ADHERENCE), result)
    }

    @Test
    fun `genau achtzig prozent adhaerenz ist anwendbar`() {
        val result = AdaptiveTdeeCalculator.evaluate(input(nutritionAdherence = 0.80))
        assertIs<AdaptiveTdeeResult.Eligible>(result)
    }

    @Test
    fun `fehlende gewichtsdaten am fensterrand sind nicht anwendbar`() {
        val result = AdaptiveTdeeCalculator.evaluate(input(smoothedWeightStartKg = null))
        assertEquals(AdaptiveTdeeResult.Ineligible(AdaptiveTdeeIneligibleReason.MISSING_WEIGHT_DATA), result)
    }

    @Test
    fun `fehlende kalorienzufuhr ist nicht anwendbar`() {
        val result = AdaptiveTdeeCalculator.evaluate(input(avgKcalIntake = null))
        assertEquals(AdaptiveTdeeResult.Ineligible(AdaptiveTdeeIneligibleReason.MISSING_WEIGHT_DATA), result)
    }

    @Test
    fun `pruefreihenfolge -- fenster vor messpunkten vor adhaerenz vor gewichtsdaten`() {
        // Mehrere Ausschlussgruende gleichzeitig -- der ZUERST geprüfte gewinnt (Fenster).
        val result = AdaptiveTdeeCalculator.evaluate(
            input(windowDays = 5, weighInsInWindow = 1, nutritionAdherence = 0.1, smoothedWeightStartKg = null),
        )
        assertEquals(AdaptiveTdeeResult.Ineligible(AdaptiveTdeeIneligibleReason.INSUFFICIENT_WINDOW), result)
    }
}
