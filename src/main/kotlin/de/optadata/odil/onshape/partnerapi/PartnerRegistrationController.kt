package de.optadata.odil.onshape.partnerapi

import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

/** SCALE-03: `/register` selbst ist der einzige oeffentliche (Key-lose) Endpunkt unterhalb von
 * `/api/partner/v1`, siehe [PartnerApiKeyFilter]. */
@RestController
@RequestMapping("/api/partner/v1")
class PartnerRegistrationController(private val keyService: PartnerApiKeyService) {

    @PostMapping("/register")
    fun register(@Valid @RequestBody request: RegisterPartnerRequest): ResponseEntity<RegisterPartnerResponse> {
        val issued = keyService.register(request.organizationName, request.contactEmail)
        return ResponseEntity.status(HttpStatus.CREATED)
            .body(RegisterPartnerResponse(apiKey = issued.plaintextKey, keyPrefix = issued.key.keyPrefix))
    }
}
