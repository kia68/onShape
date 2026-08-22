package de.optadata.odil.onshape.barcode

import org.springframework.stereotype.Service

data class AlternativeProduct(val food: FoodDetails, val fitScore: FitScoreResult)

/**
 * KONZEPT.md §7.7:
 * 1. Produktkategorie bestimmen
 * 2. Kandidaten derselben Kategorie, Nutzerpraeferenzen erfuellt (kein Allergen-/Praeferenz-Konflikt)
 * 3. Fit-Score je Kandidat im selben Tageskontext berechnen
 * 4. Sortieren, Top 3 mit Score-Delta >= 15 zeigen
 * 5. Kein Kandidat besser -> leere Liste (der Controller/Frontend formuliert die ehrliche
 *    Nachricht "eine der besseren Optionen", siehe FR-45 -- Text gehoert ins Frontend, NFR-11).
 *
 * "in DE/EU verfuegbar" (Schritt 2) ist mit dem aktuellen Schema nicht pruefbar -- es gibt kein
 * Verfuegbarkeits-/Regions-Feld auf `foods`. Bewusst ausgelassen, nicht stillschweigend erfunden.
 */
@Service
class AlternativeRecommendationService(private val foodDetailsRepository: FoodDetailsRepository) {
    companion object {
        const val MIN_SCORE_DELTA = 15
        const val MAX_ALTERNATIVES = 3
    }

    fun recommend(product: FoodDetails, currentScore: Int, profile: FitScoreProfile, day: FitScoreDayContext): List<AlternativeProduct> {
        val category = product.category ?: return emptyList()
        return foodDetailsRepository.findByCategory(category, product.id)
            .map { candidate -> candidate to FitScoreCalculator.calculate(candidate, profile, day) }
            .filter { (_, score) -> score.allergenMatches.isEmpty() && score.dietaryPreferenceConflict == null }
            .filter { (_, score) -> score.score - currentScore >= MIN_SCORE_DELTA }
            .sortedByDescending { (_, score) -> score.score }
            .take(MAX_ALTERNATIVES)
            .map { (food, score) -> AlternativeProduct(food, score) }
    }
}
