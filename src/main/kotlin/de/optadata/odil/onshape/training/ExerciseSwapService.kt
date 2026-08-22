package de.optadata.odil.onshape.training

import de.optadata.odil.onshape.onboarding.Experience
import de.optadata.odil.onshape.onboarding.ProfileRepository
import de.optadata.odil.onshape.security.RlsSession
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Service
import org.springframework.web.server.ResponseStatusException
import java.util.UUID


/** FR-74: eine Uebung im aktiven Plan ablehnen/ersetzen. Die Ablehnung fliesst dauerhaft in
 * [ExerciseScorer]s w8-Term ein (persistiert ueber [ExerciseFeedbackRepository], unabhaengig
 * davon ob eine Alternative gefunden wird). */
@Service
class ExerciseSwapService(
    private val profileRepository: ProfileRepository,
    private val exerciseRepository: ExerciseRepository,
    private val exerciseFeedbackRepository: ExerciseFeedbackRepository,
    private val programRepository: ProgramRepository,
    private val rlsSession: RlsSession,
) {

    fun swap(userId: UUID, programId: UUID, oldExerciseId: UUID, reason: SwapReason): SwapExerciseResponse = rlsSession.asUser(userId) {
        val program = programRepository.findById(programId)
            ?.takeIf { it.userId == userId }
            ?: throw ResponseStatusException(HttpStatus.NOT_FOUND, "Plan nicht gefunden")
        val day = program.days.firstOrNull { day -> day.items.any { it.exerciseId == oldExerciseId } }
            ?: throw ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY, "Uebung ist nicht Teil dieses Plans")

        val profile = profileRepository.findByUserId(userId)
            ?: throw ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY, "Onboarding noch nicht abgeschlossen")
        val pool = exerciseRepository.findAll()
        val oldExercise = pool.firstOrNull { it.id == oldExerciseId }
            ?: throw ResponseStatusException(HttpStatus.NOT_FOUND, "Uebung nicht gefunden")

        val otherIdsInDay = day.items.map { it.exerciseId }.filter { it != oldExerciseId }.toSet()
        val musclesInDay = pool.filter { it.id in otherIdsInDay }.flatMap { it.primaryMuscles }.toSet()
        val context = ScoringContext(
            equipment = profile.equipment.toSet(),
            injuries = profile.injuries.toSet(),
            maxDifficulty = if (profile.experience == Experience.NONE || profile.experience == Experience.BEGINNER) ExerciseDifficulty.BEGINNER else ExerciseDifficulty.ADVANCED,
            alreadySelectedMuscles = musclesInDay,
            alreadySelectedExerciseIds = otherIdsInDay,
            rejectionCounts = exerciseFeedbackRepository.rejectionCounts(userId),
        )
        val replacement = ExerciseScorer.findAlternative(oldExercise, pool, context)

        exerciseFeedbackRepository.record(userId, oldExerciseId, reason, replacement?.id)
        if (replacement == null) throw ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY, "Keine Alternative verfuegbar")

        programRepository.replaceExerciseInProgram(programId, oldExerciseId, replacement.id)
        val updated = programRepository.findById(programId) ?: error("Program $programId disappeared during swap")
        val namesById = pool.associate { it.id to it.name }
        SwapExerciseResponse(updated.toResponse(namesById), replacement.id, replacement.name)
    }
}
