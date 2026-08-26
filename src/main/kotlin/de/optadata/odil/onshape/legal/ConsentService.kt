package de.optadata.odil.onshape.legal

import de.optadata.odil.onshape.security.RlsSession
import org.springframework.stereotype.Service
import java.time.Clock
import java.time.Instant
import java.util.UUID

/**
 * LEGAL-11: granulare, jederzeit widerrufbare Einwilligungen (KONZEPT.md §14.1). CORE ist beim
 * ersten Schritt verpflichtend (sonst gibt es keine Rechtsgrundlage fuer die Verarbeitung von
 * Gesundheitsdaten ueberhaupt), die anderen vier sind unabhaengig davon an/abwaehlbar -- ihre
 * Ablehnung blockiert nichts in dieser Klasse, sie wird nur gespeichert und muss von den
 * jeweiligen Features selbst respektiert werden (Foto-KI/Wearable-Sync/Analytics/Marketing
 * existieren noch nicht bzw. sind eigene, spaetere Epics).
 */
@Service
class ConsentService(
    private val consentRepository: ConsentRepository,
    private val rlsSession: RlsSession,
    private val clock: Clock = Clock.systemUTC(),
) {
    fun current(userId: UUID): List<Consent> {
        val stored = rlsSession.asUser(userId) { consentRepository.findAllForUser(userId) }
        val storedByPurpose = stored.associateBy { it.purpose }
        // Zwecke ohne eigene Zeile gelten als (noch) nicht erteilt -- "nicht vorangekreuzt" (§14.1).
        return ConsentPurpose.entries.map { storedByPurpose[it] ?: Consent(it, granted = false, null, null, null) }
    }

    fun submitInitial(userId: UUID, request: ConsentsRequest): List<Consent> {
        if (!request.core) throw CoreConsentRequiredException()
        val now = Instant.now(clock)
        val grants = mapOf(
            ConsentPurpose.CORE to request.core,
            ConsentPurpose.PHOTO_AI to request.photoAi,
            ConsentPurpose.WEARABLE_SYNC to request.wearableSync,
            ConsentPurpose.ANALYTICS to request.analytics,
            ConsentPurpose.MARKETING to request.marketing,
        )
        rlsSession.asUser(userId) {
            grants.forEach { (purpose, granted) -> consentRepository.upsert(userId, purpose, granted, now) }
        }
        return current(userId)
    }

    /** Fuer spaetere Aenderungen einzelner Zwecke (§14.1: "jederzeit widerrufbar"). */
    fun update(userId: UUID, purpose: ConsentPurpose, granted: Boolean): List<Consent> {
        if (purpose == ConsentPurpose.CORE && !granted) throw CoreConsentImmutableException()
        rlsSession.asUser(userId) { consentRepository.upsert(userId, purpose, granted, Instant.now(clock)) }
        return current(userId)
    }
}
