package de.optadata.odil.onshape.wellbeing

import de.optadata.odil.onshape.security.currentUserId
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.core.Authentication
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/wellbeing")
class WellbeingController(private val wellbeingService: WellbeingService) {

    @GetMapping("/guardrail-status")
    fun guardrailStatus(authentication: Authentication): GuardrailStatusResponse =
        wellbeingService.guardrailStatus(authentication.currentUserId())

    @GetMapping("/pause-status")
    fun pauseStatus(authentication: Authentication): ResponseEntity<PauseStatusResponse> =
        wellbeingService.pauseStatus(authentication.currentUserId())
            ?.let { ResponseEntity.ok(it) }
            ?: ResponseEntity.status(HttpStatus.NOT_FOUND).build()

    @PostMapping("/pause")
    fun pause(authentication: Authentication): ResponseEntity<PauseStatusResponse> = setPaused(authentication, true)

    @PostMapping("/resume")
    fun resume(authentication: Authentication): ResponseEntity<PauseStatusResponse> = setPaused(authentication, false)

    private fun setPaused(authentication: Authentication, paused: Boolean): ResponseEntity<PauseStatusResponse> =
        wellbeingService.setPaused(authentication.currentUserId(), paused)
            ?.let { ResponseEntity.ok(it) }
            ?: ResponseEntity.status(HttpStatus.NOT_FOUND).build()
}
