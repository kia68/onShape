package de.optadata.odil.onshape.security

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.security.crypto.argon2.Argon2PasswordEncoder
import org.springframework.security.crypto.password.PasswordEncoder

/** Argon2id, wie in NFR-08 (KONZEPT.md §12) gefordert. Parameter: Spring-Security-Default (19 MiB, 2 Iterationen). */
@Configuration
class PasswordEncoderConfig {

    @Bean
    fun passwordEncoder(): PasswordEncoder = Argon2PasswordEncoder.defaultsForSpringSecurity_v5_8()
}
