package de.optadata.odil.onshape.partnerapi

import java.time.Instant
import java.util.UUID

data class PartnerApiKey(
    val id: UUID,
    val organizationName: String,
    val contactEmail: String,
    val keyPrefix: String,
    val revokedAt: Instant?,
)

/** Nur unmittelbar nach [PartnerApiKeyService.register] verfuegbar -- der Klartext-Key wird
 * nirgends gespeichert, nur sein SHA-256-Hash (siehe V19__partner_api.sql). */
data class IssuedPartnerApiKey(
    val key: PartnerApiKey,
    val plaintextKey: String,
)
