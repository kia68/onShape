package de.optadata.odil.onshape.nutrition

import de.optadata.odil.onshape.security.RlsSession
import org.springframework.stereotype.Service
import java.util.UUID

@Service
class FoodSearchService(
    private val foodSearchRepository: FoodSearchRepository,
    private val rlsSession: RlsSession,
) {
    fun search(userId: UUID, query: String, locale: String, limit: Int): List<FoodSearchResult> =
        rlsSession.asUser(userId) { foodSearchRepository.search(query, locale, userId, limit) }
}
