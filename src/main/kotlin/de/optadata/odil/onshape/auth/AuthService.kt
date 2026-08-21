package de.optadata.odil.onshape.auth

import de.optadata.odil.onshape.security.JwtService
import de.optadata.odil.onshape.security.RlsSession
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.stereotype.Service

data class AuthResult(val token: String, val user: User)

/** FR-01: Registrierung/Login per E-Mail+Passwort. Apple/Google laufen ueber Spring
 * Security OAuth2 Login direkt (siehe SecurityConfig), Passkeys sind hier noch nicht
 * implementiert (siehe docs/progress.md, offener Folgeschritt fuer FR-01). */
@Service
class AuthService(
    private val userRepository: UserRepository,
    private val rlsSession: RlsSession,
    private val passwordEncoder: PasswordEncoder,
    private val jwtService: JwtService,
) {

    fun register(request: RegisterRequest): AuthResult {
        val email = request.email.trim().lowercase()
        return rlsSession.asAuthLookup {
            if (userRepository.findByEmail(email) != null) {
                throw EmailAlreadyRegisteredException(email)
            }
            val user = userRepository.insert(email, passwordEncoder.encode(request.password), request.locale)
            AuthResult(jwtService.issue(user.id, user.email), user)
        }
    }

    fun login(request: LoginRequest): AuthResult {
        val email = request.email.trim().lowercase()
        val user = rlsSession.asAuthLookup { userRepository.findByEmail(email) } ?: throw InvalidCredentialsException()
        if (user.passwordHash == null || !passwordEncoder.matches(request.password, user.passwordHash)) {
            throw InvalidCredentialsException()
        }
        return AuthResult(jwtService.issue(user.id, user.email), user)
    }

    /** Fuer OAuth2-Login (Google/Apple): Nutzer existiert ggf. schon (frueherer Login), sonst neu anlegen. */
    fun findOrCreateOAuthUser(email: String, locale: String): User {
        val normalized = email.trim().lowercase()
        return rlsSession.asAuthLookup {
            userRepository.findByEmail(normalized) ?: userRepository.insert(normalized, null, locale)
        }
    }
}
