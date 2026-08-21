package de.optadata.odil.onshape.nutrition

import de.optadata.odil.onshape.security.currentUserId
import de.optadata.odil.onshape.web.parseEnum
import jakarta.validation.Valid
import java.time.LocalDate
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.core.Authentication
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import java.util.UUID

@RestController
@RequestMapping("/api/nutrition")
class NutritionEntryController(
    private val nutritionEntryService: NutritionEntryService,
    private val dayViewService: DayViewService,
) {

    @PostMapping("/entries")
    fun log(@Valid @RequestBody request: LogEntryApiRequest, authentication: Authentication): ResponseEntity<FoodEntryResponse> {
        val entry = nutritionEntryService.log(authentication.currentUserId(), request.toDomain())
        return ResponseEntity.status(HttpStatus.CREATED).body(entry.toResponse())
    }

    /** FR-23: mehrere Eintraege in einem Aufruf. */
    @PostMapping("/entries/batch")
    fun logBatch(@Valid @RequestBody requests: List<LogEntryApiRequest>, authentication: Authentication): ResponseEntity<List<FoodEntryResponse>> {
        val entries = nutritionEntryService.logBatch(authentication.currentUserId(), requests.map { it.toDomain() })
        return ResponseEntity.status(HttpStatus.CREATED).body(entries.map { it.toResponse() })
    }

    @DeleteMapping("/entries/{id}")
    fun delete(@PathVariable id: UUID, authentication: Authentication): ResponseEntity<Void> {
        nutritionEntryService.delete(authentication.currentUserId(), id)
        return ResponseEntity.noContent().build()
    }

    /** FR-24: Tag oder einzelne Mahlzeit kopieren. */
    @PostMapping("/entries/copy")
    fun copy(@Valid @RequestBody request: CopyEntriesRequest, authentication: Authentication): List<FoodEntryResponse> {
        val slot = request.slot?.let { parseEnum(MealSlot.entries, it, "slot") }
        return nutritionEntryService.copy(authentication.currentUserId(), request.fromDate, request.toDate, slot).map { it.toResponse() }
    }

    /** FR-20: Tagesansicht. */
    @GetMapping("/day")
    fun day(
        @RequestParam date: LocalDate,
        @RequestParam(defaultValue = "de") locale: String,
        authentication: Authentication,
    ): DayViewResponse =
        dayViewService.forDate(authentication.currentUserId(), date, locale).toResponse()

    private fun LogEntryApiRequest.toDomain() = LogFoodRequest(
        foodId = foodId,
        recipeId = recipeId,
        loggedDate = loggedDate,
        slot = parseEnum(MealSlot.entries, slot, "slot"),
        grams = grams,
        servingId = servingId,
        method = parseEnum(EntryMethod.entries, method, "method"),
        clientId = clientId,
    )
}
