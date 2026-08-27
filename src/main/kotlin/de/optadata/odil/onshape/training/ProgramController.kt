package de.optadata.odil.onshape.training

import de.optadata.odil.onshape.security.currentUserId
import de.optadata.odil.onshape.web.parseEnum
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.core.Authentication
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import java.util.UUID

@RestController
@RequestMapping("/api/training/programs")
class ProgramController(
    private val programGenerationService: ProgramGenerationService,
    private val programQueryService: ProgramQueryService,
    private val exerciseSwapService: ExerciseSwapService,
    private val volumeDashboardService: VolumeDashboardService,
    private val deloadRecommendationService: DeloadRecommendationService,
) {

    /** FR-70/71/72/73. */
    @PostMapping("/generate")
    fun generate(@Valid @RequestBody request: GenerateProgramRequest, authentication: Authentication): ResponseEntity<ProgramResponse> {
        val program = programGenerationService.generateForUser(authentication.currentUserId(), request.weeks, request.splitTypeOverride)
        return ResponseEntity.status(HttpStatus.CREATED).body(program)
    }

    /** FR-75: manuelle Plan-Erstellung. */
    @PostMapping
    fun createManual(@Valid @RequestBody request: ManualProgramRequest, authentication: Authentication): ResponseEntity<ProgramResponse> {
        val program = programQueryService.createManual(authentication.currentUserId(), request)
        return ResponseEntity.status(HttpStatus.CREATED).body(program)
    }

    @GetMapping("/active")
    fun active(authentication: Authentication): ProgramResponse = programQueryService.activeFor(authentication.currentUserId())

    @PutMapping("/{id}/active")
    fun setActive(@PathVariable id: UUID, authentication: Authentication): ProgramResponse =
        programQueryService.setActive(authentication.currentUserId(), id)

    /** FR-74. */
    @PostMapping("/{id}/items/{exerciseId}/swap")
    fun swap(
        @PathVariable id: UUID,
        @PathVariable exerciseId: UUID,
        @Valid @RequestBody request: SwapExerciseRequest,
        authentication: Authentication,
    ): SwapExerciseResponse =
        exerciseSwapService.swap(authentication.currentUserId(), id, exerciseId, parseEnum(SwapReason.entries, request.reason, "reason"))

    /** FR-77. */
    @GetMapping("/active/volume")
    fun volumeDashboard(@RequestParam(required = false) week: Int?, authentication: Authentication): VolumeDashboardResponse =
        volumeDashboardService.forActiveProgram(authentication.currentUserId(), week).toResponse()

    /** FR-79. */
    @GetMapping("/active/deload-recommendation")
    fun deloadRecommendation(authentication: Authentication): DeloadRecommendationResponse =
        deloadRecommendationService.evaluate(authentication.currentUserId())
}
