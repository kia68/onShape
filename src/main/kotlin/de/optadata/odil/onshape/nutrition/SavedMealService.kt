package de.optadata.odil.onshape.nutrition

import de.optadata.odil.onshape.security.RlsSession
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Service
import org.springframework.web.server.ResponseStatusException
import java.util.UUID

/** FR-25. */
@Service
class SavedMealService(
    private val savedMealRepository: SavedMealRepository,
    private val rlsSession: RlsSession,
) {
    fun create(userId: UUID, name: String, items: List<SavedMealItem>): SavedMeal {
        require(items.isNotEmpty()) { "Meal braucht mindestens ein Lebensmittel" }
        val id = rlsSession.asUser(userId) { savedMealRepository.insert(userId, name, items) }
        return rlsSession.asUser(userId) { savedMealRepository.findById(userId, id) } ?: error("Just-inserted saved meal $id not found")
    }

    fun listForUser(userId: UUID): List<SavedMeal> = rlsSession.asUser(userId) { savedMealRepository.findByUser(userId) }

    fun delete(userId: UUID, id: UUID) {
        val deleted = rlsSession.asUser(userId) { savedMealRepository.delete(userId, id) }
        if (!deleted) throw ResponseStatusException(HttpStatus.NOT_FOUND, "Meal nicht gefunden")
    }
}
