package de.optadata.odil.onshape.security

import jakarta.servlet.FilterChain
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.web.filter.OncePerRequestFilter

/** NFR-08: Brute-Force-/Spam-Schutz fuer die einzigen unauthentifizierten Endpunkte der API
 * (`/api/auth/login`, `/api/auth/register`) -- jeder andere API-Pfad braucht ohnehin schon ein
 * gueltiges JWT. Zaehlt pro Client-IP ([RateLimiter]), unabhaengig vom Request-Body (auch ein
 * falsches Passwort zaehlt mit, sonst waere das Limit fuer Credential-Stuffing wirkungslos). */
class AuthRateLimitFilter(private val rateLimiter: RateLimiter) : OncePerRequestFilter() {

    override fun doFilterInternal(request: HttpServletRequest, response: HttpServletResponse, filterChain: FilterChain) {
        // Bewusst `requestURI` statt `servletPath`: letzteres ist je nach Servlet-Mapping (und
        // unter MockMvc grundsaetzlich) leer/unzuverlaessig, `requestURI` dagegen immer gesetzt.
        val path = request.requestURI.removePrefix(request.contextPath)
        val isRateLimited = request.method == "POST" && (path == "/api/auth/login" || path == "/api/auth/register")

        if (isRateLimited && !rateLimiter.tryAcquire(request.remoteAddr)) {
            response.status = HttpStatus.TOO_MANY_REQUESTS.value()
            response.contentType = MediaType.APPLICATION_JSON_VALUE
            response.writer.write("""{"code":"rate_limited","message":"Zu viele Anfragen. Bitte spaeter erneut versuchen."}""")
            return
        }
        filterChain.doFilter(request, response)
    }
}
