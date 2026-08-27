package de.optadata.odil.onshape.onboarding

import de.optadata.odil.onshape.security.currentUserId
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.core.Authentication
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/onboarding")
class OnboardingController(
    private val onboardingService: OnboardingService,
    private val adaptiveTdeeService: AdaptiveTdeeService,
) {

    @PutMapping("/profile")
    fun submit(@Valid @RequestBody request: OnboardingRequest, authentication: Authentication): OnboardingResultResponse =
        onboardingService.submit(authentication.currentUserId(), request)

    @GetMapping("/result")
    fun latest(authentication: Authentication): ResponseEntity<OnboardingResultResponse> =
        onboardingService.latestResult(authentication.currentUserId())
            ?.let { ResponseEntity.ok(it) }
            ?: ResponseEntity.status(HttpStatus.NOT_FOUND).build()

    /** FR-134. */
    @GetMapping("/adaptive-tdee")
    fun adaptiveTdee(authentication: Authentication): AdaptiveTdeeResponse =
        adaptiveTdeeService.compute(authentication.currentUserId())
}
