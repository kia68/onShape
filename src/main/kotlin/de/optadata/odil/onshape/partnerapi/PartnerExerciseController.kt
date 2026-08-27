package de.optadata.odil.onshape.partnerapi

import de.optadata.odil.onshape.movement.ExerciseContentRepository
import de.optadata.odil.onshape.training.Exercise
import de.optadata.odil.onshape.training.ExerciseRepository
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.server.ResponseStatusException
import java.util.UUID

/** SCALE-03: reiner Uebungskatalog fuer Partner -- keine Personalisierung (Startgewicht,
 * Anfaenger-Einblendung aus [de.optadata.odil.onshape.movement.MovementService] braucht ein
 * Nutzerprofil, das es hier per Definition nicht gibt). `exercises`/`exercise_mistakes` tragen
 * keine RLS, siehe ExerciseContentRepository-Kommentar. Fehlender redaktioneller Content (nur
 * fuer 5 Uebungen seit Epic #8 befuellt) liefert leere Anleitungslisten statt 404 -- die Uebung
 * selbst existiert, gleiche "folgt noch"-Haltung wie im eigenen Frontend. */
@RestController
@RequestMapping("/api/partner/v1/exercises")
class PartnerExerciseController(
    private val exerciseRepository: ExerciseRepository,
    private val exerciseContentRepository: ExerciseContentRepository,
) {

    @GetMapping
    fun list(): List<PartnerExerciseSummary> = exerciseRepository.findAll().map { it.toSummary() }

    @GetMapping("/{id}")
    fun detail(@PathVariable id: UUID, @RequestParam(defaultValue = "de") locale: String): PartnerExerciseDetailResponse {
        val exercise = exerciseRepository.findById(id)
            ?: throw ResponseStatusException(HttpStatus.NOT_FOUND, "Uebung nicht gefunden")
        val content = exerciseContentRepository.findContent(id, locale)
        val mistakes = exerciseContentRepository.findMistakes(id, locale)

        return PartnerExerciseDetailResponse(
            id = exercise.id.toString(),
            slug = exercise.slug,
            name = exercise.name,
            pattern = exercise.pattern.dbValue,
            equipment = exercise.equipment,
            difficulty = exercise.difficulty.dbValue,
            primaryMuscles = exercise.primaryMuscles,
            setupSteps = content?.setupSteps.orEmpty(),
            executionSteps = content?.executionSteps.orEmpty(),
            cues = content?.cues.orEmpty(),
            commonMistakes = mistakes.map { PartnerExerciseMistake(title = it.title, whyBad = it.whyBad, fix = it.fix) },
        )
    }

    private fun Exercise.toSummary() = PartnerExerciseSummary(
        id = id.toString(),
        slug = slug,
        name = name,
        pattern = pattern.dbValue,
        equipment = equipment,
        difficulty = difficulty.dbValue,
    )
}
