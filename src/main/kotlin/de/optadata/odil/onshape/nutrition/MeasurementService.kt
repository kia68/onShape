package de.optadata.odil.onshape.nutrition

import de.optadata.odil.onshape.onboarding.BodyMeasurement
import de.optadata.odil.onshape.onboarding.BodyMeasurementInput
import de.optadata.odil.onshape.onboarding.BodyMeasurementRepository
import de.optadata.odil.onshape.security.RlsSession
import java.time.LocalDate
import java.util.UUID
import org.springframework.stereotype.Service

/** FR-30. */
@Service
class MeasurementService(
    private val bodyMeasurementRepository: BodyMeasurementRepository,
    private val rlsSession: RlsSession,
) {
    fun record(userId: UUID, input: BodyMeasurementInput): BodyMeasurement {
        rlsSession.asUser(userId) { bodyMeasurementRepository.record(userId, input) }
        return rlsSession.asUser(userId) { bodyMeasurementRepository.findHistory(userId, input.measuredOn, input.measuredOn) }
            .firstOrNull() ?: error("Just-recorded measurement on ${input.measuredOn} not found")
    }

    fun history(userId: UUID, from: LocalDate, to: LocalDate): List<BodyMeasurement> =
        rlsSession.asUser(userId) { bodyMeasurementRepository.findHistory(userId, from, to) }
}
