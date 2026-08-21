package de.optadata.odil.onshape.nutrition

import de.optadata.odil.onshape.onboarding.BodyMeasurement
import de.optadata.odil.onshape.onboarding.BodyMeasurementInput
import de.optadata.odil.onshape.security.currentUserId
import jakarta.validation.Valid
import java.time.LocalDate
import java.util.UUID
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.core.Authentication
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

data class MeasurementResponse(
    val id: UUID, val measuredOn: LocalDate, val weightKg: Double?, val bodyFatPct: Double?,
    val waistCm: Double?, val hipCm: Double?, val chestCm: Double?, val armCm: Double?, val thighCm: Double?,
)

private fun BodyMeasurement.toResponse() = MeasurementResponse(id, measuredOn, weightKg, bodyFatPct, waistCm, hipCm, chestCm, armCm, thighCm)

/** FR-30: Gewicht, Koerpermasse, Koerperfett. */
@RestController
@RequestMapping("/api/measurements")
class MeasurementController(private val measurementService: MeasurementService) {

    @PostMapping
    fun record(@Valid @RequestBody request: MeasurementRequest, authentication: Authentication): ResponseEntity<MeasurementResponse> {
        val input = BodyMeasurementInput(
            request.measuredOn, request.weightKg, request.bodyFatPct, request.waistCm,
            request.hipCm, request.chestCm, request.armCm, request.thighCm,
        )
        val result = measurementService.record(authentication.currentUserId(), input)
        return ResponseEntity.status(HttpStatus.CREATED).body(result.toResponse())
    }

    @GetMapping
    fun history(
        @RequestParam from: LocalDate,
        @RequestParam to: LocalDate,
        authentication: Authentication,
    ): List<MeasurementResponse> =
        measurementService.history(authentication.currentUserId(), from, to).map { it.toResponse() }
}
