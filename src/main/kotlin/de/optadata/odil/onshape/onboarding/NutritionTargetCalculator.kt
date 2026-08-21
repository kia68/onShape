package de.optadata.odil.onshape.onboarding

import java.time.LocalDate
import java.time.Period
import kotlin.math.roundToInt

data class NutritionTargetResult(
    val kcal: Int,
    val proteinG: Int,
    val fatG: Int,
    val carbsG: Int,
    val fiberG: Int,
    val waterMl: Int,
    val bmrKcal: Int,
    val tdeeKcal: Int,
    val tdeeSource: String,
    /** Vollstaendige Herleitung fuer den aufklappbaren Ergebnis-Screen (FR-11). */
    val calculation: Map<String, Any?>,
)

data class NutritionTargetInput(
    val sex: Sex,
    val birthDate: LocalDate,
    val heightCm: Double,
    val weightKg: Double,
    val bodyFatPct: Double?,
    val activityPal: Double,
    val goal: Goal,
    val goalRatePctWeek: Double,
    val targetWeightKg: Double?,
    val dietaryPrefs: List<String>,
)

/**
 * FR-11: Tagesziel + Herleitung, nach KONZEPT.md §7.1/§7.2.
 *
 * Trainingskalorien (§7.1 Schritt 2, `kcal_training/Woche`) fliessen bewusst NICHT ein --
 * beim Onboarding existiert noch kein Trainings-Log. TDEE ist daher zunaechst nur
 * `BMR x PAL_alltag`; die Trainings-Komponente kommt mit dem ersten geloggten Training
 * (Epic Trainings-Logging) dazu.
 */
object NutritionTargetCalculator {
    private const val KCAL_PER_KG_FAT = 7700.0

    fun calculate(input: NutritionTargetInput, today: LocalDate): NutritionTargetResult {
        val ageYears = Period.between(input.birthDate, today).years
        val bmr = Bmr.calculate(input.sex, input.weightKg, input.heightCm, ageYears, input.bodyFatPct)
        val tdeeBasis = bmr.kcal * input.activityPal

        val goalAdjustment = GoalAdjustment.calculate(input.goal, input.goalRatePctWeek, input.weightKg, tdeeBasis)
        val floor = CalorieFloor.calculate(input.sex, bmr.kcal)
        val targetKcal = maxOf(goalAdjustment.targetKcal, floor.floorKcal)

        val protein = ProteinTarget.calculate(input, ageYears)
        val fat = FatTarget.calculate(targetKcal, input.weightKg)
        val carbsG = maxOf(0, ((targetKcal - protein.proteinG * 4 - fat.fatG * 9) / 4.0).roundToInt())
        val fiber = FiberTarget.calculate(input.sex, targetKcal)
        val waterMl = (input.weightKg * 35).roundToInt()

        return NutritionTargetResult(
            kcal = targetKcal,
            proteinG = protein.proteinG,
            fatG = fat.fatG,
            carbsG = carbsG,
            fiberG = fiber,
            waterMl = waterMl,
            bmrKcal = bmr.kcal,
            tdeeKcal = tdeeBasis.roundToInt(),
            tdeeSource = bmr.source,
            calculation = mapOf(
                "ageYears" to ageYears,
                "bmr" to mapOf("kcal" to bmr.kcal, "formula" to bmr.formula, "source" to bmr.source),
                "activityPal" to input.activityPal,
                "tdeeBasisKcal" to tdeeBasis.roundToInt(),
                "trainingKcalPerWeek" to 0,
                "note_training" to "Trainingskalorien werden ab dem ersten geloggten Training ergaenzt (KONZEPT.md §7.1 Schritt 2).",
                "goal" to input.goal.dbValue,
                "goalRatePctWeek" to input.goalRatePctWeek,
                "goalAdjustmentKcal" to goalAdjustment.adjustmentKcal.roundToInt(),
                "goalAdjustmentFormula" to goalAdjustment.formula,
                "calorieFloor" to mapOf("kcal" to floor.floorKcal, "rule" to floor.rule, "applied" to (floor.floorKcal > goalAdjustment.targetKcal)),
                "targetKcal" to targetKcal,
                "protein" to mapOf("g" to protein.proteinG, "gPerKgBasis" to protein.gPerKgBasis, "basis" to protein.basis, "note" to protein.note),
                "fat" to mapOf("g" to fat.fatG, "rule" to fat.rule),
                "carbsG" to carbsG,
                "fiber" to mapOf("g" to fiber, "rule" to "14 g / 1000 kcal, Mindestens 25 g (w) / 30 g (m/divers)"),
                "waterMl" to mapOf("ml" to waterMl, "rule" to "35 ml / kg Koerpergewicht (Heuristik)"),
            ),
        )
    }
}

private data class BmrResult(val kcal: Int, val formula: String, val source: String)

private object Bmr {
    fun calculate(sex: Sex, weightKg: Double, heightCm: Double, ageYears: Int, bodyFatPct: Double?): BmrResult {
        if (bodyFatPct != null) {
            val ffm = weightKg * (1 - bodyFatPct / 100.0)
            val kcal = 370 + 21.6 * ffm
            return BmrResult(kcal.roundToInt(), "370 + 21.6 x FFM($ffm kg)", "katch_mcardle")
        }
        val kcal = when (sex) {
            Sex.MALE -> 10 * weightKg + 6.25 * heightCm - 5 * ageYears + 5
            Sex.FEMALE -> 10 * weightKg + 6.25 * heightCm - 5 * ageYears - 161
            Sex.OTHER, Sex.UNSPECIFIED -> 10 * weightKg + 6.25 * heightCm - 5 * ageYears - 78
        }
        val formula = when (sex) {
            Sex.MALE -> "10xGewicht + 6.25xGroesse - 5xAlter + 5"
            Sex.FEMALE -> "10xGewicht + 6.25xGroesse - 5xAlter - 161"
            Sex.OTHER, Sex.UNSPECIFIED -> "Mittelwert Maenner-/Frauen-Formel (10xGewicht + 6.25xGroesse - 5xAlter - 78)"
        }
        return BmrResult(kcal.roundToInt(), formula, "mifflin_st_jeor")
    }
}

private data class GoalAdjustmentResult(val targetKcal: Int, val adjustmentKcal: Double, val formula: String)

/** KONZEPT.md §7.1 Schritt 3. Auf- und Abbau nutzen dieselbe gewichtsbasierte Formel wie das
 * Defizit (KG x %Rate x 7700 / 7) statt der pauschalen "+5-10% ueber TDEE"-Beschreibung --
 * konsistent zur nutzerwaehlbaren, medizinisch begrenzten Zielrate aus FR-04. */
private object GoalAdjustment {
    fun calculate(goal: Goal, ratePctWeek: Double, weightKg: Double, tdeeBasis: Double): GoalAdjustmentResult {
        val weeklyDeltaKcal = weightKg * (ratePctWeek / 100.0) * 7700.0
        val dailyDeltaKcal = weeklyDeltaKcal / 7.0
        return when (goal) {
            Goal.LOSE -> GoalAdjustmentResult(
                (tdeeBasis - dailyDeltaKcal).roundToInt(),
                -dailyDeltaKcal,
                "TDEE - ($weightKg kg x $ratePctWeek% x 7700 / 7)",
            )
            Goal.GAIN_MUSCLE, Goal.GAIN_WEIGHT -> GoalAdjustmentResult(
                (tdeeBasis + dailyDeltaKcal).roundToInt(),
                dailyDeltaKcal,
                "TDEE + ($weightKg kg x $ratePctWeek% x 7700 / 7)",
            )
            Goal.STRENGTH, Goal.MAINTAIN, Goal.RECOMP -> GoalAdjustmentResult(tdeeBasis.roundToInt(), 0.0, "TDEE (Erhaltung)")
        }
    }
}

private data class CalorieFloorResult(val floorKcal: Int, val rule: String)

/** KONZEPT.md §7.1: "Nie unter BMR x 1.1. Nie unter 1200 kcal (w) / 1500 kcal (m)."
 * Fuer divers/keine Angabe gilt mangels eigenem Referenzwert der konservativere (hoehere) Wert. */
private object CalorieFloor {
    fun calculate(sex: Sex, bmrKcal: Int): CalorieFloorResult {
        val bmrFloor = (bmrKcal * 1.1).roundToInt()
        val absoluteFloor = if (sex == Sex.FEMALE) 1200 else 1500
        val floor = maxOf(bmrFloor, absoluteFloor)
        return CalorieFloorResult(floor, "max(BMR x 1.1 = $bmrFloor, $absoluteFloor kcal)")
    }
}

private data class ProteinResult(val proteinG: Int, val gPerKgBasis: Double, val basis: String, val note: String)

/** KONZEPT.md §7.2 Protein-Tabelle, auf Punktwerte (Korridor-Mittelwert) reduziert. */
private object ProteinTarget {
    fun calculate(input: NutritionTargetInput, ageYears: Int): ProteinResult {
        val heightM = input.heightCm / 100.0
        val currentBmi = input.weightKg / (heightM * heightM)
        val isVegan = input.dietaryPrefs.any { it.equals("vegan", ignoreCase = true) }
        val isLean = input.bodyFatPct != null &&
            ((input.sex == Sex.MALE && input.bodyFatPct < 15) || (input.sex != Sex.MALE && input.bodyFatPct < 25))

        var (gPerKg, basis, weightBasisKg) = when {
            currentBmi > 30 -> Triple(1.6, "Adipositas (BMI>30), g/kg Zielgewicht", input.targetWeightKg ?: input.weightKg)
            input.goal == Goal.LOSE && isLean -> Triple(2.55, "Defizit, schlank, g/kg fettfreie Masse", input.weightKg * (1 - input.bodyFatPct / 100.0))
            input.goal == Goal.LOSE -> Triple(2.0, "Defizit, normaler KFA, g/kg Koerpergewicht", input.weightKg)
            else -> Triple(1.8, "Erhaltung/Aufbau, g/kg Koerpergewicht", input.weightKg)
        }
        val notes = mutableListOf<String>()
        if (ageYears > 60) {
            gPerKg += 0.2
            notes += "+0.2 g/kg (>60 Jahre, anabole Resistenz)"
        }
        if (isVegan) {
            gPerKg += 0.2
            notes += "+0.2 g/kg (vegan, Verdaulichkeit/Aminosaeurenprofil)"
        }
        val proteinG = (gPerKg * weightBasisKg).roundToInt()
        return ProteinResult(proteinG, gPerKg, basis, notes.joinToString("; ").ifBlank { "keine Zuschlaege" })
    }
}

private data class FatResult(val fatG: Int, val rule: String)

/** KONZEPT.md §7.2: 20-30% der Gesamtkalorien (hier 25% Mittelwert), Untergrenze 0.6 g/kg. */
private object FatTarget {
    fun calculate(targetKcal: Int, weightKg: Double): FatResult {
        val fromPct = (targetKcal * 0.25 / 9.0).roundToInt()
        val floor = (weightKg * 0.6).roundToInt()
        val fatG = maxOf(fromPct, floor)
        return FatResult(fatG, "max(25% der Kalorien = $fromPct g, 0.6 g/kg = $floor g)")
    }
}

/** KONZEPT.md §7.2: 14 g / 1000 kcal, mindestens 25 g (w) / 30 g (m, divers/keine Angabe). */
private object FiberTarget {
    fun calculate(sex: Sex, targetKcal: Int): Int {
        val fromKcal = (targetKcal / 1000.0 * 14).roundToInt()
        val floor = if (sex == Sex.FEMALE) 25 else 30
        return maxOf(fromKcal, floor)
    }
}
