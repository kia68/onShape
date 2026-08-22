package de.optadata.odil.onshape.movement

import de.optadata.odil.onshape.onboarding.Sex
import de.optadata.odil.onshape.training.Mechanic
import kotlin.math.roundToInt

/** [reasonCode] statt Fliesstext, damit die Uebersetzung komplett im Frontend bleibt (gleiche
 * Konvention wie `FitScoreReason`, siehe dessen KDoc zu NFR-11 DE/EN-Paritaet). */
data class StartingWeightRecommendation(val weightKg: Double?, val reasonCode: String)

/**
 * FR-110 "Startgewicht-Empfehlung". KONZEPT.md §12.2 nennt nur zwei konkrete Beispiele als Prosa
 * ("Fang mit der leeren Stange an" / "Frauen mit deinem Gewicht starten meist bei 15-20 kg"),
 * keine Formel. Interpretation:
 * - Langhantel-Uebungen: immer die leere Standard-Olympia-Stange (20 kg) -- das IST das erste
 *   KONZEPT-Beispiel woertlich.
 * - Reine Koerpergewichtsuebungen: keine Zusatzlast, kein Vorschlag noetig.
 * - Kurzhantel-/Kettlebell-Uebungen: koerpergewichtsrelativer Schaetzwert (Faktor nach Mechanik
 *   und Geschlecht), auf 2,5 kg gerundet. Faktoren sind bewusst konservativ gewaehlt -- ein zu
 *   niedriger erster Vorschlag kostet eine Sekunde zum Nachjustieren, ein zu hoher kann eine
 *   Anfaengerin entmutigen oder die Form gefaehrden.
 */
object StartingWeightRecommender {
    private const val EMPTY_BARBELL_KG = 20.0
    private const val WEIGHT_STEP_KG = 2.5
    private const val COMPOUND_MALE_FACTOR = 0.20
    private const val COMPOUND_OTHER_FACTOR = 0.15
    private const val ISOLATION_MALE_FACTOR = 0.08
    private const val ISOLATION_OTHER_FACTOR = 0.06

    fun recommend(equipment: List<String>, mechanic: Mechanic, sex: Sex, bodyWeightKg: Double): StartingWeightRecommendation {
        if ("barbell" in equipment) return StartingWeightRecommendation(EMPTY_BARBELL_KG, "barbell_empty")
        if (equipment == listOf("bodyweight")) return StartingWeightRecommendation(null, "bodyweight_only")
        if ("dumbbells" in equipment || "kettlebell" in equipment) {
            // Ohne genauere Angabe (nur "male"/"female" im KONZEPT-Beispiel) werden "other" und
            // "unspecified" konservativ dem niedrigeren Faktor zugeordnet -- lieber zu leicht
            // starten als zu schwer, siehe Klassendoku.
            val isMale = sex == Sex.MALE
            val factor = when {
                mechanic == Mechanic.COMPOUND && isMale -> COMPOUND_MALE_FACTOR
                mechanic == Mechanic.COMPOUND -> COMPOUND_OTHER_FACTOR
                isMale -> ISOLATION_MALE_FACTOR
                else -> ISOLATION_OTHER_FACTOR
            }
            val rounded = ((bodyWeightKg * factor) / WEIGHT_STEP_KG).roundToInt() * WEIGHT_STEP_KG
            return StartingWeightRecommendation(rounded.coerceAtLeast(WEIGHT_STEP_KG), "bodyweight_relative")
        }
        return StartingWeightRecommendation(null, "no_recommendation")
    }
}
