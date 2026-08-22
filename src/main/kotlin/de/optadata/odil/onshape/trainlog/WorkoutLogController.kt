package de.optadata.odil.onshape.trainlog

import de.optadata.odil.onshape.security.currentUserId
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.core.Authentication
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import java.util.UUID

@RestController
@RequestMapping("/api/trainlog")
class WorkoutLogController(private val workoutLogService: WorkoutLogService) {

    /** FR-90/96: startet ein neues Workout (optional an einen Plan-Tag gebunden). */
    @PostMapping("/sessions")
    fun start(@Valid @RequestBody request: StartSessionRequest, authentication: Authentication): ResponseEntity<WorkoutSessionResponse> {
        val session = workoutLogService.startSession(authentication.currentUserId(), request.programDayId, request.clientId)
        return ResponseEntity.status(HttpStatus.CREATED).body(session.toResponse())
    }

    @GetMapping("/sessions/active")
    fun active(authentication: Authentication): WorkoutSessionResponse =
        workoutLogService.activeSession(authentication.currentUserId()).toResponse()

    @GetMapping("/sessions/history")
    fun history(@RequestParam(defaultValue = "20") limit: Int, authentication: Authentication): List<WorkoutSessionResponse> =
        workoutLogService.history(authentication.currentUserId(), limit.coerceIn(1, 100)).map { it.toResponse() }

    @GetMapping("/sessions/{id}")
    fun detail(@PathVariable id: UUID, authentication: Authentication): WorkoutSessionDetailResponse {
        val (session, sets) = workoutLogService.sessionDetail(authentication.currentUserId(), id)
        return WorkoutSessionDetailResponse(session.toResponse(), sets.map { it.toResponse() })
    }

    @PutMapping("/sessions/{id}/finish")
    fun finish(@PathVariable id: UUID, @Valid @RequestBody request: FinishSessionRequest, authentication: Authentication): WorkoutSessionResponse =
        workoutLogService.finishSession(authentication.currentUserId(), id, request.perceivedEffort, request.notes).toResponse()

    /** FR-90 (2 Taps), FR-93 (RIR), FR-98 (PR-Erkennung inline in der Antwort). */
    @PostMapping("/sessions/{id}/sets")
    fun logSet(@PathVariable id: UUID, @Valid @RequestBody request: LogSetRequest, authentication: Authentication): ResponseEntity<LogSetResponse> {
        val result = workoutLogService.logSet(authentication.currentUserId(), id, request.toInput())
        return ResponseEntity.status(HttpStatus.CREATED).body(result.toResponse())
    }

    /** FR-91: Vorbelegung fuer den naechsten Satz. `repMax`/`targetRir` kommen vom Frontend aus
     * dem geladenen Plan-Item, damit der Server nicht zusaetzlich den aktiven Plan aufloesen muss. */
    @GetMapping("/exercises/{exerciseId}/prefill")
    fun prefill(
        @PathVariable exerciseId: UUID,
        @RequestParam(required = false) repMax: Int?,
        @RequestParam(required = false) targetRir: Int?,
        authentication: Authentication,
    ): PrefillResponse = workoutLogService.prefill(authentication.currentUserId(), exerciseId, repMax, targetRir).toResponse()

    /** FR-97. */
    @GetMapping("/exercises/{exerciseId}/one-rep-max-history")
    fun oneRepMaxHistory(@PathVariable exerciseId: UUID, authentication: Authentication): List<OneRepMaxPointResponse> =
        workoutLogService.oneRepMaxHistory(authentication.currentUserId(), exerciseId).map { it.toResponse() }

    /** FR-98 als Uebersicht. */
    @GetMapping("/exercises/{exerciseId}/personal-bests")
    fun personalBests(@PathVariable exerciseId: UUID, authentication: Authentication): PersonalBestsResponse =
        workoutLogService.personalBests(authentication.currentUserId(), exerciseId).toResponse()

    /** FR-132: Uebungsauswahl fuer den Fortschritts-Screen. */
    @GetMapping("/exercises/logged")
    fun loggedExercises(authentication: Authentication): List<LoggedExerciseResponse> =
        workoutLogService.loggedExercises(authentication.currentUserId()).map { LoggedExerciseResponse(it.id, it.name) }
}
