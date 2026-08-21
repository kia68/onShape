package de.optadata.odil.onshape.web

data class ApiError(val code: String, val message: String, val fieldErrors: Map<String, String> = emptyMap())
