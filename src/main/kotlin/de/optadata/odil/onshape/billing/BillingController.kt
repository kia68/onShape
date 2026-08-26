package de.optadata.odil.onshape.billing

import de.optadata.odil.onshape.security.currentUserId
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.core.Authentication
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestHeader
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.server.ResponseStatusException

@RestController
@RequestMapping("/api/billing")
class BillingController(
    private val billingService: BillingService,
    private val subscriptionService: SubscriptionService,
    private val stripeWebhookService: StripeWebhookService,
) {

    @GetMapping("/subscription")
    fun subscription(authentication: Authentication): SubscriptionResponse =
        subscriptionService.current(authentication.currentUserId()).toResponse()

    /** BIZ-02/BIZ-03: `request.plan` ist einer der [CheckoutPlan]-Werte (z. B. "plus_monthly",
     * "lifetime"). */
    @PostMapping("/checkout")
    fun checkout(@Valid @RequestBody request: CheckoutRequest, authentication: Authentication): CheckoutResponse {
        val plan = CheckoutPlan.entries.firstOrNull { it.dbValue == request.plan }
            ?: throw ResponseStatusException(HttpStatus.BAD_REQUEST, "Ungueltiger Plan: ${request.plan}")
        val session = billingService.startCheckout(authentication.currentUserId(), plan)
        return CheckoutResponse(session.url)
    }

    @PostMapping("/portal")
    fun portal(authentication: Authentication): BillingPortalResponse =
        BillingPortalResponse(billingService.startBillingPortal(authentication.currentUserId()))

    /** Oeffentlich (siehe SecurityConfig): Stripe ruft diesen Endpunkt ohne JWT auf, die
     * `Stripe-Signature`-Pruefung uebernimmt die Authentifizierung. Rohen Body als String, nicht
     * als geparste Struktur -- die Signaturpruefung braucht exakt die Originalbytes. */
    @PostMapping("/webhook")
    fun webhook(@RequestBody payload: String, @RequestHeader("Stripe-Signature", required = false) sigHeader: String?): ResponseEntity<Void> {
        stripeWebhookService.handle(payload, sigHeader)
        return ResponseEntity.ok().build()
    }
}
