package de.optadata.odil.onshape.legal

import de.optadata.odil.onshape.security.currentUserId
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.security.core.Authentication
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.server.ResponseStatusException

@RestController
@RequestMapping("/api/consents")
class ConsentController(private val consentService: ConsentService) {

    @GetMapping
    fun current(authentication: Authentication): List<ConsentResponse> =
        consentService.current(authentication.currentUserId()).toResponse()

    /** Initialer Einwilligungsschritt (§14.1) -- alle fuenf Zwecke auf einmal, CORE verpflichtend. */
    @PutMapping
    fun submitInitial(@Valid @RequestBody request: ConsentsRequest, authentication: Authentication): List<ConsentResponse> =
        consentService.submitInitial(authentication.currentUserId(), request).toResponse()

    /** Einzelnen Zweck spaeter aendern (§14.1: "jederzeit widerrufbar"). CORE nicht widerrufbar. */
    @PutMapping("/{purpose}")
    fun update(
        @PathVariable purpose: String,
        @Valid @RequestBody request: SingleConsentRequest,
        authentication: Authentication,
    ): List<ConsentResponse> {
        val parsed = ConsentPurpose.entries.firstOrNull { it.dbValue == purpose }
            ?: throw ResponseStatusException(HttpStatus.BAD_REQUEST, "Ungueltiger Zweck: $purpose")
        return consentService.update(authentication.currentUserId(), parsed, request.granted).toResponse()
    }
}
