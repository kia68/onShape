package de.optadata.odil.onshape.movement

import de.optadata.odil.onshape.security.currentUserId
import org.springframework.security.core.Authentication
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import java.util.UUID

/** FR-110/111/114 (Bewegungsvermittlung). */
@RestController
@RequestMapping("/api/movement/exercises")
class MovementController(private val movementService: MovementService) {

    @GetMapping("/{id}")
    fun detail(
        @PathVariable id: UUID,
        @RequestParam(defaultValue = "de") locale: String,
        authentication: Authentication,
    ): ExerciseDetailResponse = movementService.detail(authentication.currentUserId(), id, locale).toResponse()
}
