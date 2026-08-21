package de.optadata.odil.onshape.foodimport

import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Service

data class ImportSummary(
    var inserted: Int = 0,
    var updated: Int = 0,
    var skippedDuplicate: Int = 0,
    var skippedImplausible: Int = 0,
)

/**
 * Orchestriert Plausibilitaetspruefung, Deduplizierung und Import eines Batches
 * (KONZEPT.md §10.4 Schritte 4-8). Schritt 9 (naechtlicher Delta-Import) haengt am
 * `@Scheduled`-Job unten, standardmaessig deaktiviert (`foodimport.nightly.enabled=false`) —
 * er soll erst aktiv laufen, wenn USDA-Key, BLS-Exportdatei etc. wirklich konfiguriert sind.
 */
@Service
class FoodImportService(
    private val repository: FoodImportRepository,
    private val sources: List<FoodSourceClient>,
    @Value("\${foodimport.dedup.fuzzy-threshold:0.6}") private val fuzzyThreshold: Double,
) {
    private val log = LoggerFactory.getLogger(FoodImportService::class.java)

    fun importBatch(items: List<ImportedFood>): ImportSummary {
        val summary = ImportSummary()
        for (item in items) {
            when (importOne(item)) {
                is DedupDecision.InsertNew, is DedupDecision.InsertHigherTrustVariant -> summary.inserted++
                is DedupDecision.UpdateExisting -> summary.updated++
                is DedupDecision.SkipLowerTrustDuplicate -> summary.skippedDuplicate++
                null -> summary.skippedImplausible++
            }
        }
        return summary
    }

    /** @return die getroffene [DedupDecision], oder null wenn wegen Implausibilitaet uebersprungen. */
    fun importOne(candidate: ImportedFood): DedupDecision? {
        val plausibility = PlausibilityChecker.evaluate(candidate)
        if (!plausibility.isPlausible) {
            log.info("Ueberspringe Kandidat {} ({}): Flags {}", candidate.sourceId, candidate.nameDe, plausibility.flags)
            return null
        }

        val sameSourceMatch = candidate.sourceId?.let { repository.findBySameSource(candidate.source, it) }
        val crossSourceMatch = sameSourceMatch?.let { null } ?: run {
            candidate.barcode?.let { repository.findByBarcode(it) }
                ?: repository.findBestFuzzyMatch(candidate.nameDe, fuzzyThreshold)
        }

        val decision = Deduplicator.decide(candidate, sameSourceMatch, crossSourceMatch)
        val trust = TrustAssigner.assign(candidate.source)
        when (decision) {
            is DedupDecision.InsertNew, is DedupDecision.InsertHigherTrustVariant ->
                repository.insert(candidate, trust)
            is DedupDecision.UpdateExisting -> repository.update(decision.existingId, candidate, trust)
            is DedupDecision.SkipLowerTrustDuplicate -> Unit
        }
        return decision
    }

    @Scheduled(cron = "\${foodimport.nightly.cron:0 0 3 * * *}")
    fun nightlyDeltaImport() {
        if (!nightlyEnabled) return
        for (client in sources) {
            val delta = client.fetchDelta()
            val summary = importBatch(delta)
            log.info("Nightly Delta-Import {}: {}", client.source, summary)
        }
    }

    @Value("\${foodimport.nightly.enabled:false}")
    private var nightlyEnabled: Boolean = false
}
