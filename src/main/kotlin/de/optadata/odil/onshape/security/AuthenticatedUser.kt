package de.optadata.odil.onshape.security

import org.springframework.security.core.Authentication
import java.util.UUID

data class AuthenticatedUser(val id: UUID, val email: String)

fun Authentication.currentUserId(): UUID = (principal as AuthenticatedUser).id
