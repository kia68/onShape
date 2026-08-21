package de.optadata.odil.onshape.nutrition

import de.optadata.odil.onshape.security.RlsSession
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Service
import org.springframework.web.server.ResponseStatusException
import java.time.LocalDate
import java.util.UUID

/** FR-29. */
@Service
class WaterService(
    private val waterEntryRepository: WaterEntryRepository,
    private val rlsSession: RlsSession,
) {
    fun log(userId: UUID, loggedDate: LocalDate, amountMl: Int, clientId: String?): WaterEntry =
        rlsSession.asUser(userId) { waterEntryRepository.insert(userId, loggedDate, amountMl, clientId) }

    fun forDate(userId: UUID, date: LocalDate): List<WaterEntry> =
        rlsSession.asUser(userId) { waterEntryRepository.findByDate(userId, date) }

    fun delete(userId: UUID, id: UUID) {
        val deleted = rlsSession.asUser(userId) { waterEntryRepository.delete(userId, id) }
        if (!deleted) throw ResponseStatusException(HttpStatus.NOT_FOUND, "Eintrag nicht gefunden")
    }
}
