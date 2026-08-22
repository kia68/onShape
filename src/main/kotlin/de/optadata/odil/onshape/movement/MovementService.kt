package de.optadata.odil.onshape.movement

import de.optadata.odil.onshape.onboarding.BodyMeasurementRepository
import de.optadata.odil.onshape.onboarding.Experience
import de.optadata.odil.onshape.onboarding.ProfileRepository
import de.optadata.odil.onshape.security.RlsSession
import de.optadata.odil.onshape.trainlog.WorkoutSetRepository
import de.optadata.odil.onshape.training.Exercise
import de.optadata.odil.onshape.training.ExerciseRepository
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Service
import org.springframework.web.server.ResponseStatusException
import java.util.UUID

data class ProgressionLink(val id: UUID, val slug: String, val name: String)

data class ExerciseDetailResult(
    val exercise: Exercise,
    val content: LocalizedExerciseContent,
    val mistakes: List<ExerciseMistake>,
    val regressionOf: ProgressionLink?,
    val progressionTo: ProgressionLink?,
    val startingWeight: StartingWeightRecommendation?,
    /** FR-111: Anfaengermodus soll die Anleitung VOR der ersten Ausfuehrung automatisch zeigen. */
    val showBeginnerIntro: Boolean,
)

@Service
class MovementService(
    private val exerciseRepository: ExerciseRepository,
    private val exerciseContentRepository: ExerciseContentRepository,
    private val profileRepository: ProfileRepository,
    private val bodyMeasurementRepository: BodyMeasurementRepository,
    private val workoutSetRepository: WorkoutSetRepository,
    private val rlsSession: RlsSession,
) {

    fun detail(userId: UUID, exerciseId: UUID, locale: String): ExerciseDetailResult {
        val exercise = exerciseRepository.findById(exerciseId)
            ?: throw ResponseStatusException(HttpStatus.NOT_FOUND, "Uebung nicht gefunden")
        val content = exerciseContentRepository.findContent(exerciseId, locale)
            ?: error("exercises-Zeile fuer $exerciseId verschwunden zwischen den beiden Abfragen")
        val mistakes = exerciseContentRepository.findMistakes(exerciseId, locale)
        val regressionOf = content.regressionOf?.let { exerciseRepository.findById(it) }?.let { ProgressionLink(it.id, it.slug, it.name) }
        val progressionTo = content.progressionTo?.let { exerciseRepository.findById(it) }?.let { ProgressionLink(it.id, it.slug, it.name) }

        val (profile, bodyWeightKg, everLogged) = rlsSession.asUser(userId) {
            Triple(
                profileRepository.findByUserId(userId),
                bodyMeasurementRepository.findLatestWeight(userId),
                workoutSetRepository.hasEverLogged(userId, exerciseId),
            )
        }

        val startingWeight = if (profile != null && bodyWeightKg != null) {
            StartingWeightRecommender.recommend(exercise.equipment, exercise.mechanic, profile.sex, bodyWeightKg)
        } else {
            null
        }
        val isBeginner = profile != null && (profile.experience == Experience.NONE || profile.experience == Experience.BEGINNER)

        return ExerciseDetailResult(
            exercise = exercise,
            content = content,
            mistakes = mistakes,
            regressionOf = regressionOf,
            progressionTo = progressionTo,
            startingWeight = startingWeight,
            showBeginnerIntro = isBeginner && !everLogged,
        )
    }
}
