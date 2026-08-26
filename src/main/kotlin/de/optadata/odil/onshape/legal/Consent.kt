package de.optadata.odil.onshape.legal

import de.optadata.odil.onshape.onboarding.EnumWithDbValue
import java.time.Instant

/**
 * KONZEPT.md §14.1 "Granularitaet": getrennte Einwilligungen fuer Kernfunktion, Foto-KI,
 * Wearable-Sync, anonymisierte Produktanalyse, Marketing. CORE ist die einzige Voraussetzung
 * fuer die App selbst (Rechtsgrundlage fuer die Verarbeitung von Gesundheitsdaten, Art. 9 Abs. 2
 * lit. a DSGVO) -- ohne sie faellt die Rechtsgrundlage fuer die Kernfunktion weg, "Ablehnung
 * einzelner Zwecke darf die Kernfunktion nicht blockieren" bezieht sich auf die anderen vier.
 */
enum class ConsentPurpose(override val dbValue: String) : EnumWithDbValue {
    CORE("core"),
    PHOTO_AI("photo_ai"),
    WEARABLE_SYNC("wearable_sync"),
    ANALYTICS("analytics"),
    MARKETING("marketing"),
}

data class Consent(
    val purpose: ConsentPurpose,
    val granted: Boolean,
    val grantedAt: Instant?,
    val revokedAt: Instant?,
    val updatedAt: Instant?,
)

class CoreConsentRequiredException :
    RuntimeException("Einwilligung fuer Kernfunktion (CORE) ist Voraussetzung fuer die Nutzung der App")

class CoreConsentImmutableException :
    RuntimeException("Einwilligung fuer Kernfunktion (CORE) kann nicht ueber diesen Weg widerrufen werden")
