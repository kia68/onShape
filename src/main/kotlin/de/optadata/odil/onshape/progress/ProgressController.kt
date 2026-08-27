package de.optadata.odil.onshape.progress

import de.optadata.odil.onshape.security.currentUserId
import org.springframework.security.core.Authentication
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import java.time.DayOfWeek
import java.time.LocalDate

/** FR-130/131/133. FR-132 (Kraftverlauf) liegt unter `/api/trainlog/exercises/{id}/one-rep-max-history`
 * (Epic Trainings-Logging) -- keine Duplizierung, siehe dortige KDoc. */
@RestController
@RequestMapping("/api/progress")
class ProgressController(private val progressService: ProgressService) {

    @GetMapping("/weight")
    fun weight(
        @RequestParam from: LocalDate,
        @RequestParam to: LocalDate,
        authentication: Authentication,
    ): WeightHistoryResponse = progressService.weightHistory(authentication.currentUserId(), from, to)

    @GetMapping("/nutrition")
    fun nutrition(
        @RequestParam from: LocalDate,
        @RequestParam to: LocalDate,
        authentication: Authentication,
    ): NutritionHistoryResponse = progressService.nutritionHistory(authentication.currentUserId(), from, to)

    @GetMapping("/volume")
    fun volume(
        @RequestParam from: LocalDate,
        @RequestParam to: LocalDate,
        authentication: Authentication,
    ): List<WeeklyMuscleVolumeResponse> = progressService.volumeHistory(authentication.currentUserId(), from, to)

    /** FR-135. [weekStart] optional -- Default ist die laufende Woche. Ein beliebiges Datum
     * wird auf den Montag derselben ISO-Woche geklemmt (nicht abgelehnt), damit das Frontend
     * kein Montags-Datepicker-Constraint bauen muss. */
    @GetMapping("/weekly-report")
    fun weeklyReport(
        @RequestParam weekStart: LocalDate?,
        authentication: Authentication,
    ): WeeklyReportResponse =
        progressService.weeklyReport(authentication.currentUserId(), (weekStart ?: LocalDate.now()).with(DayOfWeek.MONDAY))
}
