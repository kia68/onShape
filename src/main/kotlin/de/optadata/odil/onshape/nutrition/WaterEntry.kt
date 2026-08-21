package de.optadata.odil.onshape.nutrition

import java.time.Instant
import java.time.LocalDate
import java.util.UUID

data class WaterEntry(val id: UUID, val userId: UUID, val loggedDate: LocalDate, val amountMl: Int, val clientId: String?, val createdAt: Instant)
