package de.optadata.odil.onshape.training

import de.optadata.odil.onshape.onboarding.Goal
import de.optadata.odil.onshape.security.RlsSession
import de.optadata.odil.onshape.web.parseEnum
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Service
import org.springframework.web.server.ResponseStatusException
import java.util.UUID

@Service
class ProgramQueryService(
    private val exerciseRepository: ExerciseRepository,
    private val programRepository: ProgramRepository,
    private val rlsSession: RlsSession,
) {

    fun activeFor(userId: UUID): ProgramResponse {
        val program = rlsSession.asUser(userId) { programRepository.findActiveByUser(userId) }
            ?: throw ResponseStatusException(HttpStatus.NOT_FOUND, "Kein aktiver Plan")
        return program.toResponse(namesById())
    }

    /** FR-75: manuelle Plan-Erstellung. `program_items_reps_xor_duration` (V12) wird hier
     * VORAB geprueft, damit der Nutzer eine verstaendliche 422 statt eines rohen
     * Constraint-Verletzungs-Fehlers der DB sieht. */
    fun createManual(userId: UUID, request: ManualProgramRequest): ProgramResponse {
        val goal = parseEnum(Goal.entries, request.goal, "goal")
        val validExerciseIds = exerciseRepository.findAll().map { it.id }.toSet()
        for (day in request.days) {
            for (item in day.items) {
                if (item.exerciseId !in validExerciseIds) {
                    throw ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY, "Unbekannte Uebung: ${item.exerciseId}")
                }
                val hasReps = item.repMin != null && item.repMax != null
                val hasDuration = item.durationMinutes != null
                if (hasReps == hasDuration) {
                    throw ResponseStatusException(
                        HttpStatus.UNPROCESSABLE_ENTITY,
                        "Uebung ${item.exerciseId}: entweder Wiederholungsbereich oder Dauer angeben, nicht beides oder keins",
                    )
                }
            }
        }
        val programId = rlsSession.asUser(userId) { programRepository.insert(userId, request.toNewProgram(goal)) }
        val program = rlsSession.asUser(userId) { programRepository.findById(programId) } ?: error("Just-created program $programId not found")
        return program.toResponse(namesById())
    }

    fun setActive(userId: UUID, programId: UUID): ProgramResponse {
        val program = rlsSession.asUser(userId) { programRepository.findById(programId) }
            ?.takeIf { it.userId == userId }
            ?: throw ResponseStatusException(HttpStatus.NOT_FOUND, "Plan nicht gefunden")
        rlsSession.asUser(userId) { programRepository.setActive(userId, programId) }
        return program.copy(isActive = true).toResponse(namesById())
    }

    private fun namesById() = exerciseRepository.findAll().associate { it.id to it.name }
}
