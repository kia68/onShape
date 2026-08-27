package de.optadata.odil.onshape.partnerapi

import org.springframework.stereotype.Service
import java.time.Instant
import java.util.UUID

@Service
class PartnerApiKeyService(private val repository: PartnerApiKeyRepository) {

    /** SCALE-03: self-service, kein Genehmigungsschritt -- KONZEPT.md §16 nennt "oeffentliche
     * API fuer Partner" ohne weitere Details; es existiert (anders als das separate, noch nicht
     * gebaute Trainer-/Studio-Portal aus §16 Phase 3) kein Admin-/Rollenkonzept in der Codebasis,
     * das eine manuelle Freigabe tragen wuerde. [PartnerApiKeyFilter] rate-limitet sowohl die
     * Registrierung als auch jeden authentifizierten Aufruf, das ist die Missbrauchsbremse. */
    fun register(organizationName: String, contactEmail: String): IssuedPartnerApiKey {
        val plaintext = PartnerApiKeyHasher.generate()
        val id = UUID.randomUUID()
        val prefix = PartnerApiKeyHasher.displayPrefix(plaintext)
        repository.insert(id, organizationName, contactEmail, prefix, PartnerApiKeyHasher.hash(plaintext), Instant.now())
        return IssuedPartnerApiKey(
            key = PartnerApiKey(id, organizationName, contactEmail, prefix, revokedAt = null),
            plaintextKey = plaintext,
        )
    }

    fun authenticate(plaintextKey: String): PartnerApiKey? {
        val match = repository.findActiveByHash(PartnerApiKeyHasher.hash(plaintextKey)) ?: return null
        repository.touchLastUsed(match.id, Instant.now())
        return match
    }
}
