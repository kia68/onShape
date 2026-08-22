package de.optadata.odil.onshape.training

import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

/** Oeffentliche Referenzdaten (keine RLS), fuer Uebungstausch- und Manuell-Erstellen-UI. */
@RestController
@RequestMapping("/api/training/exercises")
class ExerciseController(private val exerciseRepository: ExerciseRepository) {

    @GetMapping
    fun list(): List<ExerciseResponse> = exerciseRepository.findAll().map { it.toResponse() }
}
