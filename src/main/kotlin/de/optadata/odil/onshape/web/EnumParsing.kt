package de.optadata.odil.onshape.web

import de.optadata.odil.onshape.onboarding.EnumWithDbValue
import org.springframework.http.HttpStatus
import org.springframework.web.server.ResponseStatusException

fun <E> parseEnum(values: List<E>, raw: String, field: String): E where E : Enum<E>, E : EnumWithDbValue =
    values.firstOrNull { it.dbValue == raw }
        ?: throw ResponseStatusException(HttpStatus.BAD_REQUEST, "Ungueltiger Wert fuer $field: $raw")
