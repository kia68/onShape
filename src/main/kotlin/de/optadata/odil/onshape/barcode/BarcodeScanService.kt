package de.optadata.odil.onshape.barcode

import de.optadata.odil.onshape.billing.SubscriptionService
import de.optadata.odil.onshape.billing.TierPolicy
import de.optadata.odil.onshape.nutrition.FoodEntryRepository
import de.optadata.odil.onshape.onboarding.Goal
import de.optadata.odil.onshape.onboarding.NutritionTargetRepository
import de.optadata.odil.onshape.onboarding.Profile
import de.optadata.odil.onshape.onboarding.ProfileRepository
import de.optadata.odil.onshape.security.RlsSession
import org.springframework.stereotype.Service
import java.time.Clock
import java.time.LocalDate
import java.time.ZoneOffset
import java.util.UUID

sealed interface BarcodeScanOutcome {
    /** [fitScoreGated]: BIZ-01-Monatsdeckel im Free-Tier erreicht -- [fitScore]/[alternatives]
     * werden trotzdem immer berechnet (siehe Service-KDoc), nur die Anzeige wird an der
     * Response-Grenze (dto.kt) gefiltert. */
    data class Found(val product: FoodDetails, val fitScore: FitScoreResult, val alternatives: List<AlternativeProduct>, val fitScoreGated: Boolean) : BarcodeScanOutcome
    data class NotFound(val barcode: String) : BarcodeScanOutcome
}

/**
 * FR-40/43/45: orchestriert Produktsuche, Tageskontext und Fit-Score, protokolliert jeden Scan
 * (`barcode_scans`, FR-42 treibt "haeufig gescannt, aber nicht gefunden" ueber den Barcode-Index).
 * Ohne abgeschlossenes Onboarding (kein Profil) wird ein neutraler Kontext verwendet -- der
 * Score bleibt dann groesstenteils bei den datengetriebenen Komponenten (Verarbeitungsgrad,
 * Saettigung), Ziel-/Makro-Komponenten fallen auf ihre neutralen Werte zurueck.
 *
 * BIZ-01 (§15.1 "Fit-Score & Kaufberatung: 10 Scans/Monat" im Free-Tier): der Scan selbst bleibt
 * in JEDEM Tier unbegrenzt und kostenlos (§15.3 "kostenloser Barcode-Scanner" ist ein zentrales
 * Verkaufsargument, nie einschraenkbar) -- der Score wird immer vollstaendig berechnet UND
 * geloggt (auch gegated, siehe §15.3 "Jeder Scan liefert uns Daten"). Nur die ANZEIGE von
 * Score/Begruendung/Alternativen wird jenseits des Monatsdeckels gegated (`fitScoreGated`);
 * ein erkannter Allergen-/Praeferenz-Konflikt (`allergenMatches`/`dietaryPreferenceConflict`)
 * bleibt bewusst IMMER sichtbar, unabhaengig vom Tier -- ein Sicherheitshinweis darf nicht von
 * einer Paywall abhaengen.
 */
@Service
class BarcodeScanService(
    private val foodDetailsRepository: FoodDetailsRepository,
    private val barcodeScanRepository: BarcodeScanRepository,
    private val alternativeRecommendationService: AlternativeRecommendationService,
    private val profileRepository: ProfileRepository,
    private val nutritionTargetRepository: NutritionTargetRepository,
    private val foodEntryRepository: FoodEntryRepository,
    private val subscriptionService: SubscriptionService,
    private val rlsSession: RlsSession,
    private val clock: Clock = Clock.systemUTC(),
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

        val tier = subscriptionService.currentTier(userId)
        val monthStart = LocalDate.now(clock).withDayOfMonth(1).atStartOfDay(ZoneOffset.UTC).toInstant()
        val scoredThisMonth = rlsSession.asUser(userId) { barcodeScanRepository.countFoundSince(userId, monthStart) }
        val gated = !TierPolicy.canShowFitScore(tier, scoredThisMonth)

        rlsSession.asUser(userId) {
            barcodeScanRepository.insert(userId, barcode, product.id, found = true, result.score, result.breakdown)
        }
        return BarcodeScanOutcome.Found(product, result, alternatives, gated)
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
