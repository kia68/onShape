package de.optadata.odil.onshape.nutrition

import de.optadata.odil.onshape.security.currentUserId
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

/** FR-29: Wasser-Tracking mit Tagesziel (Ziel kommt aus dem zuletzt berechneten Tagesziel,
 * siehe [de.optadata.odil.onshape.onboarding.NutritionTargetResult.waterMl]). */
@RestController
@RequestMapping("/api/nutrition/water")
class WaterController(private val waterService: WaterService) {

    @PostMapping
    fun log(@Valid @RequestBody request: WaterLogRequest, authentication: Authentication): ResponseEntity<WaterEntryResponse> {
        val entry = waterService.log(authentication.currentUserId(), request.loggedDate, request.amountMl, request.clientId)
        return ResponseEntity.status(HttpStatus.CREATED).body(entry.toResponse())
    }

    @GetMapping
    fun day(@RequestParam date: LocalDate, authentication: Authentication): WaterDayResponse {
        val entries = waterService.forDate(authentication.currentUserId(), date)
        return WaterDayResponse(date, entries.sumOf { it.amountMl }, entries.map { it.toResponse() })
    }

    @DeleteMapping("/{id}")
    fun delete(@PathVariable id: UUID, authentication: Authentication): ResponseEntity<Void> {
        waterService.delete(authentication.currentUserId(), id)
        return ResponseEntity.noContent().build()
    }
}
