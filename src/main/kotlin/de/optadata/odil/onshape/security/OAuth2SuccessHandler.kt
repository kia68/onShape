package de.optadata.odil.onshape.security

import de.optadata.odil.onshape.auth.AuthService
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.beans.factory.annotation.Value
import org.springframework.security.core.Authentication
import org.springframework.security.oauth2.core.user.OAuth2User
import org.springframework.security.web.authentication.AuthenticationSuccessHandler
import org.springframework.stereotype.Component

/**
 * FR-01 (Google/Apple): nach erfolgreichem OAuth2-Login legt [AuthService] den Nutzer bei
 * Bedarf an und die App stellt ein eigenes JWT aus -- die Frontend-SPA soll nicht Spring
 * Securitys serverseitige Session nutzen. Redirect ueber URL-Fragment (`#token=`), damit das
 * Token nicht in Server-Logs/Referrer-Headern landet.
 *
 * Erfordert konfigurierte spring.security.oauth2.client.registration-Properties (siehe
 * application.properties) -- ohne die ist dieser Pfad inaktiv, Spring Boot registriert dann
 * gar keinen /oauth2/authorization/{registrationId}-Endpunkt.
 */
@Component
class OAuth2SuccessHandler(
    private val authService: AuthService,
    private val jwtService: JwtService,
    @Value("\${app.oauth2.redirect-uri:http://localhost:3000/de/auth/callback}") private val redirectUri: String,
) : AuthenticationSuccessHandler {

    override fun onAuthenticationSuccess(request: HttpServletRequest, response: HttpServletResponse, authentication: Authentication) {
        val oAuth2User = authentication.principal as OAuth2User
        val email = oAuth2User.getAttribute<String>("email") ?: error("OAuth2-Provider lieferte keine E-Mail")
        val locale = (oAuth2User.getAttribute<String>("locale") ?: "de").take(2)
        val user = authService.findOrCreateOAuthUser(email, locale)
        val token = jwtService.issue(user.id, user.email)
        response.sendRedirect("$redirectUri#token=$token")
    }
}
