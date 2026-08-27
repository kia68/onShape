package de.optadata.odil.onshape.progress

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class WeeklyReportGeneratorTest {

    private fun input(
        sessionsCompleted: Int = 3,
        sessionsPlanned: Int = 3,
        nutritionDaysLogged: Int = 7,
        avgKcal: Double? = 2000.0,
        targetKcal: Int? = 2000,
    ) = WeeklyReportInput(sessionsCompleted, sessionsPlanned, nutritionDaysLogged, avgKcal, targetKcal)

    @Test
    fun `alles im plan ergibt on-track ohne needs-attention`() {
        val result = WeeklyReportGenerator.generate(input())
        assertEquals(Rating.GOOD, result.trainingRating)
        assertEquals(Rating.GOOD, result.nutritionLoggingRating)
        assertEquals(Rating.GOOD, result.nutritionTargetRating)
        assertEquals(WeeklyReportRecommendation.ON_TRACK, result.recommendation)
    }

    @Test
    fun `null trainings absolviert bei geplanten einheiten braucht aufmerksamkeit und hat prioritaet`() {
        // Training UND Nutrition-Logging beide schlecht -- Training gewinnt (hoechste Prioritaet).
        val result = WeeklyReportGenerator.generate(input(sessionsCompleted = 0, sessionsPlanned = 3, nutritionDaysLogged = 0, avgKcal = null))
        assertEquals(Rating.NEEDS_ATTENTION, result.trainingRating)
        assertEquals(WeeklyReportRecommendation.FOCUS_ON_TRAINING_SESSIONS, result.recommendation)
    }

    @Test
    fun `teilweise trainiert ist neutral, kein needs-attention`() {
        val result = WeeklyReportGenerator.generate(input(sessionsCompleted = 1, sessionsPlanned = 3))
        assertEquals(Rating.NEUTRAL, result.trainingRating)
    }

    @Test
    fun `kein geplantes training ist neutral, nicht bewertbar als schlecht`() {
        val result = WeeklyReportGenerator.generate(input(sessionsCompleted = 0, sessionsPlanned = 0))
        assertEquals(Rating.NEUTRAL, result.trainingRating)
    }

    @Test
    fun `unter drei von sieben tagen geloggt braucht aufmerksamkeit`() {
        val result = WeeklyReportGenerator.generate(input(nutritionDaysLogged = 2, avgKcal = 1800.0))
        assertEquals(Rating.NEEDS_ATTENTION, result.nutritionLoggingRating)
        assertEquals(WeeklyReportRecommendation.FOCUS_ON_NUTRITION_LOGGING, result.recommendation)
    }

    @Test
    fun `drei bis fuenf tage geloggt ist neutral`() {
        val result = WeeklyReportGenerator.generate(input(nutritionDaysLogged = 4, avgKcal = 1800.0))
        assertEquals(Rating.NEUTRAL, result.nutritionLoggingRating)
    }

    @Test
    fun `sechs von sieben tagen geloggt ist gut`() {
        val result = WeeklyReportGenerator.generate(input(nutritionDaysLogged = 6))
        assertEquals(Rating.GOOD, result.nutritionLoggingRating)
    }

    @Test
    fun `kcal ueber ziel wird genauso bewertet wie kcal unter ziel -- symmetrisch, keine wertung`() {
        val over = WeeklyReportGenerator.generate(input(avgKcal = 2600.0, targetKcal = 2000))
        val under = WeeklyReportGenerator.generate(input(avgKcal = 1400.0, targetKcal = 2000))
        assertEquals(Rating.NEEDS_ATTENTION, over.nutritionTargetRating)
        assertEquals(Rating.NEEDS_ATTENTION, under.nutritionTargetRating)
        assertEquals(WeeklyReportRecommendation.NUTRITION_OFF_TARGET, over.recommendation)
        assertEquals(WeeklyReportRecommendation.NUTRITION_OFF_TARGET, under.recommendation)
    }

    @Test
    fun `kcal innerhalb zehn prozent vom ziel ist gut`() {
        val result = WeeklyReportGenerator.generate(input(avgKcal = 2150.0, targetKcal = 2000))
        assertEquals(Rating.GOOD, result.nutritionTargetRating)
    }

    @Test
    fun `kcal zwischen zehn und fuenfundzwanzig prozent abweichung ist neutral`() {
        val result = WeeklyReportGenerator.generate(input(avgKcal = 2300.0, targetKcal = 2000))
        assertEquals(Rating.NEUTRAL, result.nutritionTargetRating)
    }

    @Test
    fun `ohne geloggte tage ist die ziel-bewertung nicht beurteilbar`() {
        val result = WeeklyReportGenerator.generate(input(nutritionDaysLogged = 0, avgKcal = null))
        assertNull(result.nutritionTargetRating)
    }

    @Test
    fun `ohne tagesziel ist die ziel-bewertung nicht beurteilbar`() {
        val result = WeeklyReportGenerator.generate(input(targetKcal = null))
        assertNull(result.nutritionTargetRating)
    }

    @Test
    fun `training vor logging vor ziel-praezision -- prioritaetsreihenfolge der einen empfehlung`() {
        // Logging UND Ziel-Praezision beide schlecht, Training aber gut -> Logging gewinnt.
        val result = WeeklyReportGenerator.generate(input(nutritionDaysLogged = 1, avgKcal = 3000.0, targetKcal = 2000))
        assertEquals(WeeklyReportRecommendation.FOCUS_ON_NUTRITION_LOGGING, result.recommendation)
    }
}
