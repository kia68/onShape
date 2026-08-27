package de.optadata.odil.onshape.security

import de.optadata.odil.onshape.partnerapi.PartnerApiKeyFilter
import de.optadata.odil.onshape.partnerapi.PartnerApiKeyService
import org.springframework.beans.factory.ObjectProvider
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.http.HttpStatus
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.config.http.SessionCreationPolicy
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository
import org.springframework.security.web.SecurityFilterChain
import org.springframework.security.web.authentication.HttpStatusEntryPoint
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter
import org.springframework.web.cors.CorsConfiguration
import org.springframework.web.cors.CorsConfigurationSource
import org.springframework.web.cors.UrlBasedCorsConfigurationSource

/**
 * Stateless JWT-API. OAuth2-Login (Google/Apple, FR-01) wird nur aktiviert, wenn
 * `spring.security.oauth2.client.registration.*` konfiguriert ist -- Spring Boot registriert
 * dann automatisch einen [ClientRegistrationRepository]-Bean, sonst nicht (siehe
 * OAuth2SuccessHandler-Kommentar). So startet die App auch ohne echte Provider-Credentials.
 */
@Configuration
class SecurityConfig(
    private val jwtService: JwtService,
    private val oAuth2SuccessHandler: OAuth2SuccessHandler,
    private val clientRegistrationRepository: ObjectProvider<ClientRegistrationRepository>,
    private val partnerApiKeyService: PartnerApiKeyService,
    @Value("\${app.cors.allowed-origins:http://localhost:3000}") private val allowedOrigins: List<String>,
    @Value("\${app.security.auth-rate-limit.max-requests:30}") private val authRateLimitMaxRequests: Int,
    @Value("\${app.security.auth-rate-limit.window-seconds:60}") private val authRateLimitWindowSeconds: Long,
    @Value("\${app.partner-api.registration-rate-limit.max-requests:5}") private val partnerRegistrationMaxRequests: Int,
    @Value("\${app.partner-api.registration-rate-limit.window-seconds:3600}") private val partnerRegistrationWindowSeconds: Long,
    @Value("\${app.partner-api.call-rate-limit.max-requests:120}") private val partnerCallMaxRequests: Int,
    @Value("\${app.partner-api.call-rate-limit.window-seconds:60}") private val partnerCallWindowSeconds: Long,
) {

    @Bean
    fun filterChain(http: HttpSecurity): SecurityFilterChain {
        http
            .cors { it.configurationSource(corsConfigurationSource()) }
            .csrf { it.disable() }
            .sessionManagement { it.sessionCreationPolicy(SessionCreationPolicy.STATELESS) }
            .exceptionHandling { it.authenticationEntryPoint(HttpStatusEntryPoint(HttpStatus.UNAUTHORIZED)) }
            .authorizeHttpRequests { auth ->
                auth
                    // /api/billing/webhook: Stripe ruft ohne JWT auf, die Stripe-Signature-Pruefung
                    // im Controller uebernimmt die Authentifizierung (BIZ-02). /api/partner/v1/**:
                    // eigener Key-basierter Auth-Pfad ueber PartnerApiKeyFilter (SCALE-03), kein JWT.
                    .requestMatchers("/api/auth/**", "/actuator/health", "/api/billing/webhook", "/api/partner/v1/**")
                    .permitAll()
                    .anyRequest().authenticated()
            }
            .addFilterBefore(
                AuthRateLimitFilter(RateLimiter(authRateLimitMaxRequests, authRateLimitWindowSeconds)),
                UsernamePasswordAuthenticationFilter::class.java,
            )
            .addFilterBefore(JwtAuthenticationFilter(jwtService), UsernamePasswordAuthenticationFilter::class.java)
            .addFilterBefore(
                PartnerApiKeyFilter(
                    partnerApiKeyService,
                    registrationLimiter = RateLimiter(partnerRegistrationMaxRequests, partnerRegistrationWindowSeconds),
                    callLimiter = RateLimiter(partnerCallMaxRequests, partnerCallWindowSeconds),
                ),
                UsernamePasswordAuthenticationFilter::class.java,
            )

        if (clientRegistrationRepository.ifAvailable != null) {
            http.oauth2Login { it.successHandler(oAuth2SuccessHandler) }
        }

        return http.build()
    }

    private fun corsConfigurationSource(): CorsConfigurationSource {
        val config = CorsConfiguration().apply {
            allowedOrigins = this@SecurityConfig.allowedOrigins
            allowedMethods = listOf("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS")
            allowedHeaders = listOf("Authorization", "Content-Type")
        }
        return UrlBasedCorsConfigurationSource().apply { registerCorsConfiguration("/**", config) }
    }
}
