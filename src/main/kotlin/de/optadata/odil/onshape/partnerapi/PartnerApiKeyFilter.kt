package de.optadata.odil.onshape.partnerapi

import de.optadata.odil.onshape.security.RateLimiter
import jakarta.servlet.FilterChain
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.web.filter.OncePerRequestFilter

/**
 * SCALE-03: eigener, JWT-unabhaengiger Auth-/Rate-Limit-Pfad fuer alle `/api/partner/v1`-Pfade, gleiches
 * Grundmuster wie [de.optadata.odil.onshape.security.AuthRateLimitFilter] -- schreibt die
 * Fehlerantwort direkt, statt ueber Spring-Security-`Authentication` zu gehen (analog zum
 * Stripe-Webhook, der auch ausserhalb des JWT-Modells authentifiziert, siehe SecurityConfig).
 * `/register` selbst braucht keinen Key (self-service, PartnerApiKeyService-KDoc), wird aber
 * ueber ein eigenes, engeres IP-Limit vor Massen-Erzeugung von Keys geschuetzt.
 */
class PartnerApiKeyFilter(
    private val keyService: PartnerApiKeyService,
    private val registrationLimiter: RateLimiter,
    private val callLimiter: RateLimiter,
) : OncePerRequestFilter() {

    override fun doFilterInternal(request: HttpServletRequest, response: HttpServletResponse, filterChain: FilterChain) {
        val path = request.requestURI.removePrefix(request.contextPath)
        if (!path.startsWith("/api/partner/v1/")) {
            filterChain.doFilter(request, response)
            return
        }

        if (path == "/api/partner/v1/register") {
            if (!registrationLimiter.tryAcquire(request.remoteAddr)) {
                reject(response, HttpStatus.TOO_MANY_REQUESTS, "rate_limited", "Zu viele Registrierungen. Bitte spaeter erneut versuchen.")
                return
            }
            filterChain.doFilter(request, response)
            return
        }

        val header = request.getHeader("Authorization")
        val rawKey = header?.takeIf { it.startsWith("Bearer ") }?.removePrefix("Bearer ")?.trim()
        if (rawKey.isNullOrEmpty()) {
            reject(response, HttpStatus.UNAUTHORIZED, "missing_api_key", "Authorization: Bearer <api-key> fehlt.")
            return
        }

        val key = keyService.authenticate(rawKey)
        if (key == null) {
            reject(response, HttpStatus.UNAUTHORIZED, "invalid_api_key", "Unbekannter oder widerrufener API-Key.")
            return
        }

        if (!callLimiter.tryAcquire(key.id.toString())) {
            reject(response, HttpStatus.TOO_MANY_REQUESTS, "rate_limited", "Zu viele Anfragen. Bitte spaeter erneut versuchen.")
            return
        }

        filterChain.doFilter(request, response)
    }

    private fun reject(response: HttpServletResponse, status: HttpStatus, code: String, message: String) {
        response.status = status.value()
        response.contentType = MediaType.APPLICATION_JSON_VALUE
        response.writer.write("""{"code":"$code","message":"$message"}""")
    }
}
