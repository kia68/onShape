package de.optadata.odil.onshape.onboarding

import java.time.LocalDate
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class NutritionTargetCalculatorTest {

    private val today = LocalDate.of(2026, 1, 1)

    private fun baseInput(
        sex: Sex = Sex.MALE,
        birthDate: LocalDate = LocalDate.of(1996, 1, 1), // 30 Jahre
        heightCm: Double = 180.0,
        weightKg: Double = 80.0,
        bodyFatPct: Double? = null,
        activityPal: Double = 1.40,
        goal: Goal = Goal.MAINTAIN,
        goalRatePctWeek: Double = 0.0,
        targetWeightKg: Double? = null,
        dietaryPrefs: List<String> = emptyList(),
    ) = NutritionTargetInput(sex, birthDate, heightCm, weightKg, bodyFatPct, activityPal, goal, goalRatePctWeek, targetWeightKg, dietaryPrefs)

    @Test
    fun `bmr fuer maenner nach Mifflin-St Jeor`() {
        // BMR = 10*80 + 6.25*180 - 5*30 + 5 = 800 + 1125 - 150 + 5 = 1780
        val result = NutritionTargetCalculator.calculate(baseInput(), today)
        assertEquals(1780, result.bmrKcal)
        assertEquals("mifflin_st_jeor", result.tdeeSource)
    }

    @Test
    fun `bmr fuer frauen nach Mifflin-St Jeor`() {
        // BMR = 10*65 + 6.25*165 - 5*28 - 161 = 650 + 1031.25 - 140 - 161 = 1380.25 -> 1380
        val input = baseInput(sex = Sex.FEMALE, birthDate = LocalDate.of(1998, 1, 1), heightCm = 165.0, weightKg = 65.0)
        val result = NutritionTargetCalculator.calculate(input, today)
        assertEquals(1380, result.bmrKcal)
    }

    @Test
    fun `bmr fuer divers ist Mittelwert aus Maenner- und Frauenformel`() {
        val male = NutritionTargetCalculator.calculate(baseInput(sex = Sex.MALE), today).bmrKcal
        val female = NutritionTargetCalculator.calculate(baseInput(sex = Sex.FEMALE), today).bmrKcal
        val other = NutritionTargetCalculator.calculate(baseInput(sex = Sex.OTHER), today).bmrKcal
        val unspecified = NutritionTargetCalculator.calculate(baseInput(sex = Sex.UNSPECIFIED), today).bmrKcal
        assertEquals((male + female) / 2.0, other.toDouble(), 1.0)
        assertEquals(other, unspecified)
    }

    @Test
    fun `katch-mcardle greift wenn Koerperfettanteil bekannt ist`() {
        // FFM = 80 * (1 - 0.20) = 64; BMR = 370 + 21.6*64 = 1752.4 -> 1752
        val result = NutritionTargetCalculator.calculate(baseInput(bodyFatPct = 20.0), today)
        assertEquals(1752, result.bmrKcal)
        assertEquals("katch_mcardle", result.tdeeSource)
    }

    @Test
    fun `tdee ist bmr mal aktivitaetsfaktor ohne trainingskalorien`() {
        val result = NutritionTargetCalculator.calculate(baseInput(activityPal = 1.55), today)
        assertEquals((1780 * 1.55).toInt(), result.tdeeKcal)
        assertEquals(0, result.calculation["trainingKcalPerWeek"])
    }

    @Test
    fun `abnehmen erzeugt defizit nach KG x Rate x 7700 durch 7`() {
        val result = NutritionTargetCalculator.calculate(baseInput(goal = Goal.LOSE, goalRatePctWeek = 0.5, activityPal = 1.40), today)
        val tdee = 1780 * 1.40
        val deficit = 80.0 * 0.005 * 7700.0 / 7.0
        val expected = (tdee - deficit).toInt()
        assertTrue(result.kcal in (expected - 1)..(expected + 1))
        assertTrue(result.kcal < tdee.toInt())
    }

    @Test
    fun `zunehmen erzeugt ueberschuss`() {
        val result = NutritionTargetCalculator.calculate(baseInput(goal = Goal.GAIN_WEIGHT, goalRatePctWeek = 0.3, activityPal = 1.40), today)
        val tdee = 1780 * 1.40
        assertTrue(result.kcal > tdee.toInt())
    }

    @Test
    fun `erhaltung entspricht tdee`() {
        val result = NutritionTargetCalculator.calculate(baseInput(goal = Goal.MAINTAIN, activityPal = 1.40), today)
        assertEquals((1780 * 1.40).toInt(), result.kcal)
    }

    @Test
    fun `kalorienziel wird nie unter BMR x 1,1 abgesenkt`() {
        // Sehr niedriger PAL + hohe Abnehmrate wuerde ohne Floor unter BMR*1.1 fallen.
        val result = NutritionTargetCalculator.calculate(
            baseInput(goal = Goal.LOSE, goalRatePctWeek = 1.0, activityPal = 1.10, weightKg = 100.0),
            today,
        )
        val bmr = result.bmrKcal
        assertTrue(result.kcal >= (bmr * 1.1).toInt())
    }

    @Test
    fun `kalorienziel unterschreitet nie die absolute Untergrenze fuer Frauen`() {
        val result = NutritionTargetCalculator.calculate(
            baseInput(sex = Sex.FEMALE, goal = Goal.LOSE, goalRatePctWeek = 1.0, activityPal = 1.10, weightKg = 45.0, heightCm = 150.0, birthDate = LocalDate.of(2000, 1, 1)),
            today,
        )
        assertTrue(result.kcal >= 1200)
    }

    @Test
    fun `kalorienziel unterschreitet nie die absolute Untergrenze fuer Maenner`() {
        val result = NutritionTargetCalculator.calculate(
            baseInput(sex = Sex.MALE, goal = Goal.LOSE, goalRatePctWeek = 1.0, activityPal = 1.10, weightKg = 55.0, heightCm = 160.0, birthDate = LocalDate.of(2000, 1, 1)),
            today,
        )
        assertTrue(result.kcal >= 1500)
    }

    @Test
    fun `protein steigt bei ueber 60-jaehrigen um 0,2 g pro kg`() {
        val younger = NutritionTargetCalculator.calculate(baseInput(birthDate = LocalDate.of(1990, 1, 1)), today)
        val older = NutritionTargetCalculator.calculate(baseInput(birthDate = LocalDate.of(1960, 1, 1)), today)
        assertTrue(older.proteinG > younger.proteinG)
    }

    @Test
    fun `protein steigt bei veganer Ernaehrung`() {
        val omnivore = NutritionTargetCalculator.calculate(baseInput(), today)
        val vegan = NutritionTargetCalculator.calculate(baseInput(dietaryPrefs = listOf("vegan")), today)
        assertTrue(vegan.proteinG > omnivore.proteinG)
    }

    @Test
    fun `protein nutzt Zielgewicht statt Istgewicht bei Adipositas`() {
        // BMI = 110 / 1.8^2 = 33.95 -> Adipositas-Zweig, Basis ist targetWeightKg
        val result = NutritionTargetCalculator.calculate(
            baseInput(weightKg = 110.0, targetWeightKg = 90.0, goal = Goal.LOSE, goalRatePctWeek = 0.5),
            today,
        )
        assertEquals((1.6 * 90.0).toInt(), result.proteinG)
    }

    @Test
    fun `fett bleibt nie unter der 0,6 g pro kg untergrenze`() {
        val result = NutritionTargetCalculator.calculate(
            baseInput(sex = Sex.FEMALE, goal = Goal.LOSE, goalRatePctWeek = 1.0, weightKg = 45.0, heightCm = 150.0, birthDate = LocalDate.of(2000, 1, 1)),
            today,
        )
        assertTrue(result.fatG >= (45.0 * 0.6).toInt())
    }

    @Test
    fun `carbs sind niemals negativ`() {
        val result = NutritionTargetCalculator.calculate(
            baseInput(sex = Sex.FEMALE, goal = Goal.LOSE, goalRatePctWeek = 1.0, weightKg = 40.0, heightCm = 150.0, bodyFatPct = 10.0, birthDate = LocalDate.of(2000, 1, 1)),
            today,
        )
        assertTrue(result.carbsG >= 0)
    }

    @Test
    fun `ballaststoffe folgen 14g pro 1000kcal mit Mindestwert`() {
        val result = NutritionTargetCalculator.calculate(baseInput(activityPal = 1.40), today)
        val expected = maxOf((result.kcal / 1000.0 * 14).toInt(), 30)
        assertTrue(result.fiberG in (expected - 1)..(expected + 1))
    }

    @Test
    fun `wasserziel ist 35ml pro kg koerpergewicht`() {
        val result = NutritionTargetCalculator.calculate(baseInput(weightKg = 80.0), today)
        assertEquals(2800, result.waterMl)
    }

    @Test
    fun `calculation map enthaelt vollstaendige Herleitung fuer den Ergebnis-Screen`() {
        val result = NutritionTargetCalculator.calculate(baseInput(goal = Goal.LOSE, goalRatePctWeek = 0.5), today)
        assertTrue(result.calculation.containsKey("bmr"))
        assertTrue(result.calculation.containsKey("calorieFloor"))
        assertTrue(result.calculation.containsKey("protein"))
        assertTrue(result.calculation.containsKey("fat"))
        assertTrue(result.calculation.containsKey("fiber"))
    }
}
