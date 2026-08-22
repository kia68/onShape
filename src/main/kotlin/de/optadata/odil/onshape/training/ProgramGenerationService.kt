package de.optadata.odil.onshape.training

import de.optadata.odil.onshape.onboarding.ProfileRepository
import de.optadata.odil.onshape.security.RlsSession
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Service
import org.springframework.web.server.ResponseStatusException
import java.time.LocalDate
import java.time.Period
import java.util.UUID

/** FR-70: Plangenerierung aus dem Onboarding-Profil. */
@Service
class ProgramGenerationService(
    private val profileRepository: ProfileRepository,
    private val exerciseRepository: ExerciseRepository,
    private val exerciseFeedbackRepository: ExerciseFeedbackRepository,
    private val programRepository: ProgramRepository,
    private val rlsSession: RlsSession,
) {

    fun generateForUser(userId: UUID, weeks: Int, splitTypeOverride: String? = null, today: LocalDate = LocalDate.now()): ProgramResponse {
        val profile = rlsSession.asUser(userId) { profileRepository.findByUserId(userId) }
            ?: throw ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY, "Onboarding noch nicht abgeschlossen")
        val age = Period.between(profile.birthDate, today).years
        val pool = exerciseRepository.findAll()
        val rejectionCounts = rlsSession.asUser(userId) { exerciseFeedbackRepository.rejectionCounts(userId) }

        val input = ProgramGeneratorInput(
            goal = profile.goal,
            experience = profile.experience,
            age = age,
            equipment = profile.equipment,
            injuries = profile.injuries,
            trainingDaysWeek = profile.trainingDaysWeek,
            sessionMinutes = profile.sessionMinutes,
            weeks = weeks,
            splitTypeOverride = splitTypeOverride,
        )
        val generated = ProgramGenerator.generate(input, pool, rejectionCounts)
        val newProgram = generated.toNewProgram(profile.goal)

        val programId = rlsSession.asUser(userId) { programRepository.insert(userId, newProgram) }
        val program = rlsSession.asUser(userId) { programRepository.findById(programId) } ?: error("Just-generated program $programId not found")
        return program.toResponse(pool.associate { it.id to it.name })
    }

    private fun GeneratedProgram.toNewProgram(goal: de.optadata.odil.onshape.onboarding.Goal) = NewProgram(
        name = "Trainingsplan",
        goal = goal,
        daysPerWeek = daysPerWeek,
        weeks = weeks,
        splitType = splitType,
        generatedBy = "algorithm_v1",
        generationContext = mapOf("splitType" to splitType, "daysPerWeek" to daysPerWeek, "weeks" to weeks),
        days = days.map { day ->
            NewProgramDay(
                weekNumber = day.weekNumber,
                dayIndex = day.dayIndex,
                name = day.label,
                isDeload = day.isDeload,
                items = day.items.map { item ->
                    NewProgramItem(item.exerciseId, item.sortOrder, item.sets, item.repMin, item.repMax, item.durationMinutes, item.targetRir, item.restSeconds)
                },
            )
        },
    )
}
