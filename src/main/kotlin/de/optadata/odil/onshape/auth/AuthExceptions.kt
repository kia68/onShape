package de.optadata.odil.onshape.auth

class EmailAlreadyRegisteredException(email: String) : RuntimeException("E-Mail bereits registriert: $email")

class InvalidCredentialsException : RuntimeException("E-Mail oder Passwort falsch")
