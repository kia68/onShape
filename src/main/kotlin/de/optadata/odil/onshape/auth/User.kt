package de.optadata.odil.onshape.auth

import java.time.Instant
import java.util.UUID

/** Spiegelt `users` aus V1__extensions_users_profile.sql. */
data class User(
    val id: UUID,
    val email: String,
    val passwordHash: String?,
    val locale: String,
    val unitSystem: String,
    val timezone: String,
    val createdAt: Instant,
)
