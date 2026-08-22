package de.optadata.odil.onshape.barcode

import de.optadata.odil.onshape.onboarding.Goal
import kotlin.math.roundToInt

/** Nutzerkontext fuer den Fit-Score, unabhaengig von der Onboarding-Domain gehalten, damit
 * dieses Modul isoliert testbar bleibt. */
data class FitScoreProfile(
    val goal: Goal,
    val dietaryPrefs: List<String>,
    val allergens: List<String>,
)

/** Tagesstand zum Zeitpunkt des Scans. Ziele koennen fehlen (noch kein Onboarding-Ergebnis). */
data class FitScoreDayContext(
    val targetKcal: Int?,
    val targetProteinG: Int?,
    val targetCarbsG: Int?,
    val targetFatG: Int?,
    val consumedKcal: Double,
    val consumedProteinG: Double,
    val consumedCarbsG: Double,
    val consumedFatG: Double,
) {
    val remainingKcal: Double? get() = targetKcal?.let { it - consumedKcal }
    val remainingProteinG: Double? get() = targetProteinG?.let { it - consumedProteinG }
    val remainingCarbsG: Double? get() = targetCarbsG?.let { it - consumedCarbsG }
    val remainingFatG: Double? get() = targetFatG?.let { it - consumedFatG }
}

/** Ein einzelner Erklaerungsbaustein fuer FR-44 ("Klartext-Begruendung"). Absichtlich als
 * (code, Parameter) statt als fertiger Satz: die App ist DE/EN-parallel (NFR-11), die
 * Uebersetzung gehoert ins Frontend (next-intl), nicht in den Rechenkern. [weight] ist der
 * Betrag, um den dieser Grund den Score beeinflusst hat -- fuers Sortieren im Frontend. */
data class FitScoreReason(val code: String, val params: Map<String, Any>, val weight: Double)

data class FitScoreResult(
    /** 0..100, oder exakt 0 bei Allergen-/Praeferenz-Malus (siehe [FitScoreCalculator.MALUS_ALLERGEN]). */
    val score: Int,
    val allergenMatches: List<String>,
    val dietaryPreferenceConflict: String?,
    val reasons: List<FitScoreReason>,
    /** Fuer `barcode_scans.score_breakdown` (Erklaerbarkeit, siehe V7-Migrationskommentar). */
    val breakdown: Map<String, Any?>,
)

/**
 * KONZEPT.md §7.6: FitScore = 100 x (0.30*ZielPassung + 0.25*Naehrstoffdichte
 * + 0.20*MakroBeitrag + 0.15*Verarbeitungsgrad + 0.10*Saettigungsindex) - Malus.
 *
 * Die Prosa-Spec in §7.6 beschreibt Komponenten, keine exakten Formeln -- die folgenden
 * Umsetzungen sind eine explizite, dokumentierte Interpretation (analog zu
 * [de.optadata.odil.onshape.onboarding.NutritionTargetCalculator], wo KONZEPT.md konkrete
 * Formeln vorgibt und hier nur die Prinzipien). Jede Annahme ist einzeln kommentiert.
 */
object FitScoreCalculator {
    private const val WEIGHT_GOAL_FIT = 0.30
    private const val WEIGHT_NUTRIENT_DENSITY = 0.25
    private const val WEIGHT_MACRO_CONTRIBUTION = 0.20
    private const val WEIGHT_PROCESSING = 0.15
    private const val WEIGHT_SATIETY = 0.10

    const val MALUS_ALLERGEN = 100.0 // erzwingt score = 0, siehe unten
    private const val MALUS_HIGH_SUGAR = 25.0
    private const val MALUS_TRANS_FAT = 20.0

    fun calculate(product: FoodDetails, profile: FitScoreProfile, day: FitScoreDayContext): FitScoreResult {
        val allergenMatches = product.allergens.filter { productAllergen ->
            profile.allergens.any { it.equals(productAllergen, ignoreCase = true) }
        }
        val dietaryConflict = DietaryPreferenceCheck.violatedPreference(product, profile.dietaryPrefs)

        val servingGrams = product.defaultServingGrams
        val goalFit = GoalFitComponent.score(product, profile.goal, day, servingGrams)
        val nutrientDensity = NutrientDensityComponent.score(product)
        val macroContribution = MacroContributionComponent.score(product, day, servingGrams)
        val processing = ProcessingComponent.score(product.novaGroup)
        val satiety = SatietyComponent.score(product)

        val baseScore = 100.0 * (
            WEIGHT_GOAL_FIT * goalFit.value +
                WEIGHT_NUTRIENT_DENSITY * nutrientDensity.value +
                WEIGHT_MACRO_CONTRIBUTION * macroContribution.value +
                WEIGHT_PROCESSING * processing.value +
                WEIGHT_SATIETY * satiety.value
            )

        val sugarMalus = SugarMalus.evaluate(product, day, servingGrams)
        val transFatMalus = if ((product.transFatGPer100g ?: 0.0) > 0.0) MALUS_TRANS_FAT else 0.0

        val forcedZero = allergenMatches.isNotEmpty() || dietaryConflict != null
        val score = if (forcedZero) 0 else (baseScore - sugarMalus.amount - transFatMalus).coerceIn(0.0, 100.0).roundToInt()

        val reasons = buildList {
            allergenMatches.forEach { add(FitScoreReason("allergen_warning", mapOf("allergen" to it), MALUS_ALLERGEN)) }
            dietaryConflict?.let { add(FitScoreReason("dietary_preference_conflict", mapOf("preference" to it), MALUS_ALLERGEN)) }
            if (!forcedZero) {
                add(goalFit.reason)
                add(nutrientDensity.reason)
                add(macroContribution.reason)
                add(processing.reason)
                add(satiety.reason)
                sugarMalus.reason?.let { add(it) }
                if (transFatMalus > 0) add(FitScoreReason("trans_fat_present", mapOf("transFatG" to (product.transFatGPer100g ?: 0.0)), transFatMalus))
            }
        }.sortedByDescending { it.weight }

        return FitScoreResult(
            score = score,
            allergenMatches = allergenMatches,
            dietaryPreferenceConflict = dietaryConflict,
            reasons = reasons,
            breakdown = mapOf(
                "goalFit" to goalFit.value,
                "nutrientDensity" to nutrientDensity.value,
                "macroContribution" to macroContribution.value,
                "processing" to processing.value,
                "satiety" to satiety.value,
                "baseScore" to baseScore,
                "sugarMalus" to sugarMalus.amount,
                "transFatMalus" to transFatMalus,
                "servingGrams" to servingGrams,
            ),
        )
    }
}

private data class ComponentScore(val value: Double, val reason: FitScoreReason)

/**
 * "Passt die Kaloriendichte zum Ziel? Defizit -> niedrige Energiedichte belohnt; Aufbau ->
 * hohe Energiedichte belohnt. Skaliert am verbleibenden Tagesbudget."
 *
 * Umsetzung: Mittelwert aus (a) wie gut die Energiedichte (kcal/100g, gedeckelt bei 600 als
 * "sehr dicht") zum Ziel passt, und (b) ob die Portion ins verbleibende Tagesbudget passt.
 * Ohne Tagesziel (kein Onboarding-Ergebnis) ist (b) neutral (0.5).
 */
private object GoalFitComponent {
    private const val DENSITY_CAP_KCAL = 600.0

    fun score(product: FoodDetails, goal: Goal, day: FitScoreDayContext, servingGrams: Double): ComponentScore {
        val density = (product.kcalPer100g / DENSITY_CAP_KCAL).coerceIn(0.0, 1.0)
        val densityScore = when (goal) {
            Goal.LOSE -> 1.0 - density
            Goal.GAIN_MUSCLE, Goal.GAIN_WEIGHT -> density
            Goal.STRENGTH, Goal.MAINTAIN, Goal.RECOMP -> (1.0 - 2.0 * kotlin.math.abs(density - 0.4)).coerceIn(0.0, 1.0)
        }

        val kcalForServing = product.kcalPer100g * servingGrams / 100.0
        val remaining = day.remainingKcal
        val budgetScore = when {
            remaining == null -> 0.5
            remaining <= 0 -> 0.0
            else -> (1.0 - kcalForServing / remaining).coerceIn(0.0, 1.0)
        }

        val value = (densityScore + budgetScore) / 2.0
        val reasonCode = if (value >= 0.5) "fits_energy_goal" else "energy_goal_mismatch"
        return ComponentScore(
            value,
            FitScoreReason(reasonCode, mapOf("kcalPer100g" to product.kcalPer100g, "kcalForServing" to kcalForServing), WEIGHT_GOAL_FIT_FOR_REASON * value),
        )
    }
}
private const val WEIGHT_GOAL_FIT_FOR_REASON = 30.0

/**
 * "Mikronaehrstoffe pro 100 kcal, normiert auf Referenzwerte." Referenzwerte: EU-NRV
 * (Nutrient Reference Values, Lebensmittelinformationsverordnung Anhang XIII). Schwelle: 15 %
 * des Tages-NRV pro 100 kcal gilt als maximale Dichte (1.0) -- ein einzelnes Lebensmittel, das
 * mehr liefert, ist eine seltene Ausnahme (z. B. Innereien), kein Kalibrierungsziel.
 *
 * Ohne Mikronaehrstoffdaten (aktuell fuer die meisten importierten Produkte der Fall, siehe
 * docs/progress.md -- die OFF-Anbindung liefert bislang keine Mikronaehrstoffe) ist der Wert
 * neutral (0.5), nicht 0: fehlende Daten sind kein Beleg fuer geringe Naehrstoffdichte.
 */
private object NutrientDensityComponent {
    // EU-NRV, mg bzw. µg pro Tag.
    private val NRV = mapOf(
        "iron_mg" to 14.0, "zinc_mg" to 10.0, "calcium_mg" to 800.0, "magnesium_mg" to 375.0,
        "potassium_mg" to 2000.0, "vitamin_c_mg" to 80.0, "vitamin_d_ug" to 5.0,
        "vitamin_b12_ug" to 2.5, "folate_ug" to 200.0, "sodium_mg" to 2000.0,
    )
    private const val DENSITY_THRESHOLD_FRACTION = 0.15

    fun score(product: FoodDetails): ComponentScore {
        if (product.micros.isEmpty()) {
            return ComponentScore(0.5, FitScoreReason("nutrient_density_unknown", emptyMap(), 0.0))
        }
        val contributions = product.micros.mapNotNull { (key, per100g) ->
            val nrv = NRV[key] ?: return@mapNotNull null
            val per100kcal = if (product.kcalPer100g > 0) per100g / product.kcalPer100g * 100.0 else 0.0
            (per100kcal / (nrv * DENSITY_THRESHOLD_FRACTION)).coerceIn(0.0, 1.0)
        }
        val value = if (contributions.isEmpty()) 0.5 else contributions.average()
        val reasonCode = if (value >= 0.5) "nutrient_dense" else "low_nutrient_density"
        return ComponentScore(value, FitScoreReason(reasonCode, mapOf("microCount" to contributions.size), WEIGHT_NUTRIENT_DENSITY_FOR_REASON * value))
    }
}
private const val WEIGHT_NUTRIENT_DENSITY_FOR_REASON = 25.0

/**
 * "Deckt das Produkt ein Makro ab, bei dem der Nutzer heute im Rueckstand ist? Hoher
 * Proteingehalt bei Proteindefizit -> starker Bonus." Protein wird gegenueber Kohlenhydrat/Fett
 * hoeher gewichtet (0.6/0.2/0.2), weil die Spec Protein explizit als Beispiel nennt und Protein
 * im Alltag der haeufigere Engpass ist (KONZEPT.md §7.2). Ohne Tagesziel neutral (0.5).
 */
private object MacroContributionComponent {
    private const val PROTEIN_WEIGHT = 0.6
    private const val CARBS_WEIGHT = 0.2
    private const val FAT_WEIGHT = 0.2

    fun score(product: FoodDetails, day: FitScoreDayContext, servingGrams: Double): ComponentScore {
        if (day.targetProteinG == null) {
            return ComponentScore(0.5, FitScoreReason("macro_contribution_unknown", emptyMap(), 0.0))
        }
        val factor = servingGrams / 100.0
        val proteinContribution = macroScore(product.proteinGPer100g * factor, day.remainingProteinG, day.targetProteinG)
        val carbsContribution = macroScore(product.carbsGPer100g * factor, day.remainingCarbsG, day.targetCarbsG)
        val fatContribution = macroScore(product.fatGPer100g * factor, day.remainingFatG, day.targetFatG)

        val value = PROTEIN_WEIGHT * proteinContribution + CARBS_WEIGHT * carbsContribution + FAT_WEIGHT * fatContribution
        val proteinGForServing = product.proteinGPer100g * factor
        val reasonCode = if (proteinContribution >= 0.5) "covers_protein_deficit" else "low_macro_contribution"
        return ComponentScore(
            value,
            FitScoreReason(reasonCode, mapOf("proteinG" to proteinGForServing, "remainingProteinG" to (day.remainingProteinG ?: 0.0)), WEIGHT_MACRO_FOR_REASON * value),
        )
    }

    /**
     * Bonus = wie sehr der Nutzer noch im Rueckstand ist (`deficitFraction`) MAL wie viel davon
     * diese Portion deckt (`coverageOfRemaining`), beides gedeckelt bei 1.0. Beide Faktoren sind
     * noetig: ohne `deficitFraction` wuerde ein winziger Rest-Bedarf (z. B. noch 1 g Protein
     * offen) durch eine beliebige Portion sofort auf 1.0 springen ("gedeckt"), obwohl der
     * Nutzer sein Ziel laengst erreicht hat und der Bonus dort keinen Sinn ergibt -- "bei
     * Proteindefizit" heisst: der Bonus muss mit der Groesse des Defizits selbst wachsen, nicht
     * nur mit dem Deckungsgrad einer kleinen Restmenge.
     */
    private fun macroScore(gramsInServing: Double, remaining: Double?, target: Int?): Double {
        if (target == null || target <= 0) return 0.5
        val rem = remaining ?: return 0.5
        if (rem <= 0) return 0.0
        val deficitFraction = (rem / target).coerceIn(0.0, 1.0)
        val coverageOfRemaining = (gramsInServing / rem).coerceIn(0.0, 1.0)
        return deficitFraction * coverageOfRemaining
    }
}
private const val WEIGHT_MACRO_FOR_REASON = 20.0

/** "NOVA-Klassifikation ... NOVA 4 -> Abzug, NOVA 1 -> Bonus." Lineare Abbildung 1..4 -> 1.0..0.0. */
private object ProcessingComponent {
    fun score(novaGroup: Int?): ComponentScore {
        if (novaGroup == null) return ComponentScore(0.5, FitScoreReason("processing_unknown", emptyMap(), 0.0))
        val value = ((4 - novaGroup) / 3.0).coerceIn(0.0, 1.0)
        val reasonCode = if (novaGroup >= 4) "highly_processed" else if (novaGroup <= 1) "minimally_processed" else "moderately_processed"
        return ComponentScore(value, FitScoreReason(reasonCode, mapOf("novaGroup" to novaGroup), WEIGHT_PROCESSING_FOR_REASON * value))
    }
}
private const val WEIGHT_PROCESSING_FOR_REASON = 15.0

/**
 * "Geschaetzt aus Protein-, Ballaststoff- und Wassergehalt vs. Energiedichte." Ohne
 * Wassergehalt-Feld im Schema: Heuristik aus Protein (x4) und Ballaststoffen (x7, hoeherer
 * Sattheits-Beitrag pro Gramm als Protein) relativ zur Energiedichte, gedeckelt bei 1.0.
 * Ausdruecklich eine Naeherung, keine validierte Formel (z. B. kein Holt-Saettigungsindex,
 * der klinische Studien braeuchte).
 */
private object SatietyComponent {
    fun score(product: FoodDetails): ComponentScore {
        val fiberG = product.fiberGPer100g ?: 0.0
        val raw = (product.proteinGPer100g * 4.0 + fiberG * 7.0) / (product.kcalPer100g + 1.0)
        val value = raw.coerceIn(0.0, 1.0)
        val reasonCode = if (value >= 0.5) "high_satiety" else "low_satiety"
        return ComponentScore(value, FitScoreReason(reasonCode, mapOf("fiberG" to fiberG, "proteinG" to product.proteinGPer100g), WEIGHT_SATIETY_FOR_REASON * value))
    }
}
private const val WEIGHT_SATIETY_FOR_REASON = 10.0

/**
 * "Ueber 90% Tages-Zuckerbudgets in einer Portion -> -25." Das Zuckerbudget selbst ist in
 * KONZEPT.md nicht separat definiert (nur Makros, nicht Zucker) -- abgeleitet nach WHO-Leitlinie
 * (freier Zucker < 10% der Energie) aus dem Tages-Kalorienziel: budget_g = targetKcal * 0.10 / 4.
 * Ohne Kalorienziel keine Bewertung moeglich.
 */
private object SugarMalus {
    private const val SUGAR_ENERGY_FRACTION = 0.10
    private const val KCAL_PER_GRAM_SUGAR = 4.0
    private const val THRESHOLD_FRACTION = 0.9
    private const val MALUS_AMOUNT = 25.0

    data class Result(val amount: Double, val reason: FitScoreReason?)

    fun evaluate(product: FoodDetails, day: FitScoreDayContext, servingGrams: Double): Result {
        val sugarG = product.sugarGPer100g ?: return Result(0.0, null)
        val targetKcal = day.targetKcal ?: return Result(0.0, null)
        val sugarBudgetG = targetKcal * SUGAR_ENERGY_FRACTION / KCAL_PER_GRAM_SUGAR
        if (sugarBudgetG <= 0) return Result(0.0, null)
        val sugarInServing = sugarG * servingGrams / 100.0
        val pctOfBudget = sugarInServing / sugarBudgetG
        if (pctOfBudget < THRESHOLD_FRACTION) return Result(0.0, null)
        return Result(MALUS_AMOUNT, FitScoreReason("high_sugar_share", mapOf("sugarG" to sugarInServing, "pctOfBudget" to (pctOfBudget * 100).roundToInt()), MALUS_AMOUNT))
    }
}

/**
 * "Ernaehrungspraeferenz verletzt (z. B. Gelatine bei Vegetarier) -> 0." Ohne Zutatenliste im
 * Schema (nur `allergens`/`additives`-Tags, siehe V2__foods.sql) ist nur ein Teil davon
 * pruefbar: vegan wird durch die EU-Allergentags Milch/Ei/Fisch/Krebstiere/Weichtiere verletzt,
 * vegetarisch durch Fisch/Krebstiere/Weichtiere. "Enthaelt Fleisch" laesst sich mit den
 * aktuellen Daten NICHT zuverlaessig erkennen (Fleisch ist kein EU-Pflichtallergen und daher
 * nicht in `allergens` getaggt) -- diese Luecke ist bewusst dokumentiert, nicht stillschweigend
 * "geloest".
 */
private object DietaryPreferenceCheck {
    private val VEGAN_VIOLATING_ALLERGENS = setOf("milk", "eggs", "fish", "crustaceans", "molluscs")
    private val VEGETARIAN_VIOLATING_ALLERGENS = setOf("fish", "crustaceans", "molluscs")

    fun violatedPreference(product: FoodDetails, dietaryPrefs: List<String>): String? {
        val prefs = dietaryPrefs.map { it.lowercase() }
        val allergens = product.allergens.map { it.lowercase() }.toSet()
        if ("vegan" in prefs && allergens.any { it in VEGAN_VIOLATING_ALLERGENS }) return "vegan"
        if ("vegetarian" in prefs && allergens.any { it in VEGETARIAN_VIOLATING_ALLERGENS }) return "vegetarian"
        return null
    }
}
