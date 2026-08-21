package de.optadata.odil.onshape.nutrition

import de.optadata.odil.onshape.security.currentUserId
import org.springframework.security.core.Authentication
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/foods")
class FoodSearchController(private val foodSearchService: FoodSearchService) {

    /** FR-22: Quick-Add-Suche. */
    @GetMapping("/search")
    fun search(
        @RequestParam q: String,
        @RequestParam(defaultValue = "de") locale: String,
        @RequestParam(defaultValue = "20") limit: Int,
        authentication: Authentication,
    ): List<FoodSearchResultResponse> =
        foodSearchService.search(authentication.currentUserId(), q, locale, limit.coerceIn(1, 50)).map { it.toResponse() }
}
