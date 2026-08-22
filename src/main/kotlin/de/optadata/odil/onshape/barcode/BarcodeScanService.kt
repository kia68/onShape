package de.optadata.odil.onshape.barcode

import de.optadata.odil.onshape.nutrition.FoodEntryRepository
import de.optadata.odil.onshape.onboarding.Goal
import de.optadata.odil.onshape.onboarding.NutritionTargetRepository
import de.optadata.odil.onshape.onboarding.Profile
import de.optadata.odil.onshape.onboarding.ProfileRepository
import de.optadata.odil.onshape.security.RlsSession
import org.springframework.stereotype.Service
import java.time.LocalDate
import java.util.UUID

sealed interface BarcodeScanOutcome {
    data class Found(val product: FoodDetails, val fitScore: FitScoreResult, val alternatives: List<AlternativeProduct>) : BarcodeScanOutcome
    data class NotFound(val barcode: String) : BarcodeScanOutcome
}

/**
 * FR-40/43/45: orchestriert Produktsuche, Tageskontext und Fit-Score, protokolliert jeden Scan
 * (`barcode_scans`, FR-42 treibt "haeufig gescannt, aber nicht gefunden" ueber den Barcode-Index).
 * Ohne abgeschlossenes Onboarding (kein Profil) wird ein neutraler Kontext verwendet -- der
 * Score bleibt dann groesstenteils bei den datengetriebenen Komponenten (Verarbeitungsgrad,
 * Saettigung), Ziel-/Makro-Komponenten fallen auf ihre neutralen Werte zurueck.
 */
@Service
class BarcodeScanService(
    private val foodDetailsRepository: FoodDetailsRepository,
    private val barcodeScanRepository: BarcodeScanRepository,
    private val alternativeRecommendationService: AlternativeRecommendationService,
    private val profileRepository: ProfileRepository,
    private val nutritionTargetRepository: NutritionTargetRepository,
    private val foodEntryRepository: FoodEntryRepository,
    private val rlsSession: RlsSession,
) {

    fun scan(userId: UUID, barcode: String, date: LocalDate): BarcodeScanOutcome {
        val product = foodDetailsRepository.findByBarcode(barcode)
        if (product == null) {
            rlsSession.asUser(userId) { barcodeScanRepository.insert(userId, barcode, null, found = false, null, null) }
            return BarcodeScanOutcome.NotFound(barcode)
        }

        val (fitScoreProfile, dayContext) = rlsSession.asUser(userId) { buildContext(userId, date) }
        val result = FitScoreCalculator.calculate(product, fitScoreProfile, dayContext)
        val alternatives = alternativeRecommendationService.recommend(product, result.score, fitScoreProfile, dayContext)

        rlsSession.asUser(userId) {
            barcodeScanRepository.insert(userId, barcode, product.id, found = true, result.score, result.breakdown)
        }
        return BarcodeScanOutcome.Found(product, result, alternatives)
    }

    private fun buildContext(userId: UUID, date: LocalDate): Pair<FitScoreProfile, FitScoreDayContext> {
        val profile: Profile? = profileRepository.findByUserId(userId)
        val target = nutritionTargetRepository.findLatest(userId)?.result
        val entriesToday = foodEntryRepository.findByDate(userId, date)

        val fitScoreProfile = FitScoreProfile(
            goal = profile?.goal ?: Goal.MAINTAIN,
            dietaryPrefs = profile?.dietaryPrefs ?: emptyList(),
            allergens = profile?.allergens ?: emptyList(),
        )
        val dayContext = FitScoreDayContext(
            targetKcal = target?.kcal,
            targetProteinG = target?.proteinG,
            targetCarbsG = target?.carbsG,
            targetFatG = target?.fatG,
            consumedKcal = entriesToday.sumOf { it.kcal },
            consumedProteinG = entriesToday.sumOf { it.proteinG },
            consumedCarbsG = entriesToday.sumOf { it.carbsG },
            consumedFatG = entriesToday.sumOf { it.fatG },
        )
        return fitScoreProfile to dayContext
    }
}
