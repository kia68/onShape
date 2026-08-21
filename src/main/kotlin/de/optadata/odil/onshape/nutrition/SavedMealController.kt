package de.optadata.odil.onshape.nutrition

import de.optadata.odil.onshape.security.currentUserId
import de.optadata.odil.onshape.web.parseEnum
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.core.Authentication
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.util.UUID

/** FR-25: eigene, wiederverwendbare Mahlzeiten. */
@RestController
@RequestMapping("/api/nutrition/meals")
class SavedMealController(
    private val savedMealService: SavedMealService,
    private val nutritionEntryService: NutritionEntryService,
) {

    @PostMapping
    fun create(@Valid @RequestBody request: CreateSavedMealRequest, authentication: Authentication): ResponseEntity<SavedMealResponse> {
        val meal = savedMealService.create(authentication.currentUserId(), request.name, request.items.map { SavedMealItem(it.foodId, it.grams) })
        return ResponseEntity.status(HttpStatus.CREATED).body(meal.toResponse())
    }

    @GetMapping
    fun list(authentication: Authentication): List<SavedMealResponse> =
        savedMealService.listForUser(authentication.currentUserId()).map { it.toResponse() }

    @DeleteMapping("/{id}")
    fun delete(@PathVariable id: UUID, authentication: Authentication): ResponseEntity<Void> {
        savedMealService.delete(authentication.currentUserId(), id)
        return ResponseEntity.noContent().build()
    }

    /** Alle Positionen des Meals auf einmal verbuchen. */
    @PostMapping("/{id}/log")
    fun log(@PathVariable id: UUID, @Valid @RequestBody request: LogSavedMealRequest, authentication: Authentication): ResponseEntity<List<FoodEntryResponse>> {
        val slot = parseEnum(MealSlot.entries, request.slot, "slot")
        val entries = nutritionEntryService.logSavedMeal(authentication.currentUserId(), id, request.loggedDate, slot)
        return ResponseEntity.status(HttpStatus.CREATED).body(entries.map { it.toResponse() })
    }
}
