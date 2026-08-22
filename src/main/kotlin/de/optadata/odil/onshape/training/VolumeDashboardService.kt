package de.optadata.odil.onshape.training

import de.optadata.odil.onshape.onboarding.ProfileRepository
import de.optadata.odil.onshape.security.RlsSession
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Service
import org.springframework.web.server.ResponseStatusException
import java.time.LocalDate
import java.time.Period
import java.util.UUID

enum class VolumeStatus { UNDER, IN_RANGE, OVER }

data class MuscleVolumeEntry(val muscle: String, val plannedSets: Double, val corridorMin: Int, val corridorMax: Int, val status: VolumeStatus)

data class VolumeDashboard(val weekNumber: Int, val isDeload: Boolean, val entries: List<MuscleVolumeEntry>)

/**
 * FR-77. `weekly_muscle_volume` (Materialized View aus [de.optadata.odil.onshape.training])
 * fasst GELOGGTE `workout_sets` zusammen -- die gibt es erst ab dem Trainings-Logging-Epic. Bis
 * dahin wird das Dashboard aus dem GEPLANTEN aktiven Programm berechnet (`program_items`), was
 * dem Nutzer schon waehrend der Planerstellung zeigt, ob der Split die Volumen-Korridore trifft.
 * Cardio-Slots (`duration_minutes` statt Wiederholungen) zaehlen nicht als Satz-Volumen.
 */
@Service
class VolumeDashboardService(
    private val profileRepository: ProfileRepository,
    private val exerciseRepository: ExerciseRepository,
    private val programRepository: ProgramRepository,
    private val rlsSession: RlsSession,
) {

    fun forActiveProgram(userId: UUID, weekNumber: Int?, today: LocalDate = LocalDate.now()): VolumeDashboard {
        val profile = rlsSession.asUser(userId) { profileRepository.findByUserId(userId) }
            ?: throw ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY, "Onboarding noch nicht abgeschlossen")
        val program = rlsSession.asUser(userId) { programRepository.findActiveByUser(userId) }
            ?: throw ResponseStatusException(HttpStatus.NOT_FOUND, "Kein aktiver Plan")
        val week = (weekNumber ?: 1).coerceIn(1, program.weeks)
        val days = program.days.filter { it.weekNumber == week }
        val isDeload = days.any { it.isDeload }

        val exercisesById = exerciseRepository.findAll().associateBy { it.id }
        val setsByMuscle = mutableMapOf<String, Double>()
        for (day in days) {
            for (item in day.items) {
                if (item.durationMinutes != null) continue
                val exercise = exercisesById[item.exerciseId] ?: continue
                for (m in exercise.muscles) {
                    setsByMuscle.merge(m.muscle, item.sets * m.factor, Double::plus)
                }
            }
        }

        val age = Period.between(profile.birthDate, today).years
        val corridor = VolumeCorridor.forProfile(profile.experience, age)
        val entries = setsByMuscle.entries
            .sortedByDescending { it.value }
            .map { (muscle, sets) ->
                val status = when {
                    sets < corridor.startSetsPerMuscle -> VolumeStatus.UNDER
                    sets > corridor.maxSetsPerMuscle -> VolumeStatus.OVER
                    else -> VolumeStatus.IN_RANGE
                }
                MuscleVolumeEntry(muscle, sets, corridor.startSetsPerMuscle, corridor.maxSetsPerMuscle, status)
            }
        return VolumeDashboard(week, isDeload, entries)
    }
}
