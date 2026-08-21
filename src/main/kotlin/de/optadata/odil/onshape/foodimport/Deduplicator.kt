package de.optadata.odil.onshape.foodimport

import java.util.UUID

/** Projektion einer bestehenden `foods`-Zeile, so weit fuer den Dedup-Entscheid noetig. */
data class ExistingFood(
    val id: UUID,
    val source: FoodSource,
    val sourceId: String?,
    val trust: TrustLevel,
)

sealed interface DedupDecision {
    /** Kein bestehender Treffer — neue Zeile anlegen. */
    data object InsertNew : DedupDecision

    /** Gleiche Quelle + gleiche source_id: idempotenter Re-Import, bestehende Zeile aktualisieren. */
    data class UpdateExisting(val existingId: UUID) : DedupDecision

    /**
     * Barcode-/Fuzzy-Treffer aus ANDERER Quelle mit gleich- oder hoeherer Vertrauensstufe:
     * Kandidat nicht importieren. Die bestehende Zeile bleibt die massgebliche.
     */
    data class SkipLowerTrustDuplicate(val existingId: UUID) : DedupDecision

    /**
     * Barcode-/Fuzzy-Treffer aus ANDERER Quelle, Kandidat hat aber hoehere Vertrauensstufe:
     * Trotzdem als NEUE Zeile anlegen (eigene source-Partition, siehe ODbL-Hinweis unten).
     * Die Rangfolge zwischen beiden Zeilen wird erst zur Laufzeit beim Lesen aufgeloest
     * (ORDER BY trust) — nie durch Merge oder Ueberschreiben der bestehenden Zeile.
     */
    data class InsertHigherTrustVariant(val existingId: UUID) : DedupDecision
}

/**
 * Deduplizierung aus KONZEPT.md §10.4 Schritt 4.
 *
 * Wichtig fuer die ODbL-Auflage (§10.4, "Konsequenz fuer die Architektur"): OFF-Daten
 * duerfen nie in dieselbe Zeile wie proprietaere Daten (BLS/USDA/eigene Verified-Layer)
 * gemerged werden. Diese Klasse merged deshalb NIE Zeilen unterschiedlicher Quellen —
 * sie entscheidet nur, ob eine neue Zeile angelegt, eine Zeile der GLEICHEN Quelle
 * aktualisiert, oder ein niedrigwertigerer Duplikat-Kandidat uebersprungen wird.
 */
object Deduplicator {

    const val DEFAULT_FUZZY_SIMILARITY_THRESHOLD = 0.6

    /**
     * @param candidate zu importierendes Lebensmittel
     * @param sameSourceMatch bestehende Zeile mit identischer (source, sourceId) — Re-Import
     * @param crossSourceMatch bester Barcode- oder Trigram-Fuzzy-Treffer aus einer ANDEREN Quelle
     */
    fun decide(
        candidate: ImportedFood,
        sameSourceMatch: ExistingFood?,
        crossSourceMatch: ExistingFood?,
    ): DedupDecision {
        if (sameSourceMatch != null) {
            return DedupDecision.UpdateExisting(sameSourceMatch.id)
        }
        if (crossSourceMatch != null) {
            val candidateTrust = TrustAssigner.assign(candidate.source)
            return if (candidateTrust > crossSourceMatch.trust) {
                DedupDecision.InsertHigherTrustVariant(crossSourceMatch.id)
            } else {
                DedupDecision.SkipLowerTrustDuplicate(crossSourceMatch.id)
            }
        }
        return DedupDecision.InsertNew
    }
}
