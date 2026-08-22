package de.optadata.odil.onshape.progress

import de.optadata.odil.onshape.nutrition.FoodEntry
import de.optadata.odil.onshape.nutrition.FoodEntryRepository
import de.optadata.odil.onshape.nutrition.WaterEntry
import de.optadata.odil.onshape.nutrition.WaterEntryRepository
import de.optadata.odil.onshape.onboarding.BodyMeasurement
import de.optadata.odil.onshape.onboarding.BodyMeasurementRepository
import de.optadata.odil.onshape.onboarding.Profile
import de.optadata.odil.onshape.onboarding.ProfileRepository
import de.optadata.odil.onshape.security.RlsSession
import de.optadata.odil.onshape.training.Program
import de.optadata.odil.onshape.training.ProgramRepository
import de.optadata.odil.onshape.trainlog.WorkoutSession
import de.optadata.odil.onshape.trainlog.WorkoutSessionRepository
import de.optadata.odil.onshape.trainlog.WorkoutSet
import de.optadata.odil.onshape.trainlog.WorkoutSetRepository
import org.springframework.stereotype.Service
import java.io.ByteArrayOutputStream
import java.time.Instant
import java.util.UUID
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

data class WorkoutSessionExport(val session: WorkoutSession, val sets: List<WorkoutSet>)

data class ExportData(
    val exportedAt: Instant,
    val profile: Profile?,
    val bodyMeasurements: List<BodyMeasurement>,
    val foodEntries: List<FoodEntry>,
    val waterEntries: List<WaterEntry>,
    val workoutSessions: List<WorkoutSessionExport>,
    val programs: List<Program>,
)

/**
 * FR-137: "vollstaendig, kostenlos, im Free-Tier" (DSGVO Art. 20). Deckt die Kern-Trainings-/
 * Ernaehrungsdaten ab, die dieser Nutzer selbst erzeugt hat (Profil, Koerpermasse, Ernaehrungs-
 * log, Trainingshistorie, eigene Plaene). Bewusst NICHT enthalten: gespeicherte Meals/Rezepte
 * und der Barcode-Scan-Verlauf -- Referenz-/Katalogdaten mit geringerem Portabilitaetswert als
 * die eigentlichen Tracking-Daten, aus Zeitgruenden in dieser Session zurueckgestellt (gleiche
 * Art Scope-Entscheidung wie an anderen Stellen dieses Projekts, siehe docs/progress.md).
 *
 * Der JSON-Export serialisiert bewusst die ROHEN Domain-Objekte statt eigener Response-DTOs --
 * Vollstaendigkeit ist hier wichtiger als die sonst uebliche `.dbValue`-Normalisierung von Enums
 * (die JSON-Datei zeigt deshalb z.B. `"MALE"` statt `"male"`, inhaltlich identisch).
 */
@Service
class ExportService(
    private val profileRepository: ProfileRepository,
    private val bodyMeasurementRepository: BodyMeasurementRepository,
    private val foodEntryRepository: FoodEntryRepository,
    private val waterEntryRepository: WaterEntryRepository,
    private val workoutSessionRepository: WorkoutSessionRepository,
    private val workoutSetRepository: WorkoutSetRepository,
    private val programRepository: ProgramRepository,
    private val rlsSession: RlsSession,
) {
    private val wideFrom = java.time.LocalDate.of(1900, 1, 1)
    private val wideTo = java.time.LocalDate.of(2100, 1, 1)

    fun exportData(userId: UUID): ExportData = rlsSession.asUser(userId) {
        val sessions = workoutSessionRepository.findAllForUser(userId)
        ExportData(
            exportedAt = Instant.now(),
            profile = profileRepository.findByUserId(userId),
            bodyMeasurements = bodyMeasurementRepository.findHistory(userId, wideFrom, wideTo),
            foodEntries = foodEntryRepository.findAllForUser(userId),
            waterEntries = waterEntryRepository.findAllForUser(userId),
            workoutSessions = sessions.map { WorkoutSessionExport(it, workoutSetRepository.findBySession(it.id)) },
            programs = programRepository.findAllForUser(userId),
        )
    }

    fun exportCsvZip(userId: UUID): ByteArray {
        val data = exportData(userId)
        val out = ByteArrayOutputStream()
        ZipOutputStream(out).use { zip ->
            data.profile?.let { p ->
                zip.writeCsv(
                    "profile.csv",
                    listOf("sex", "birthDate", "heightCm", "experience", "activityPal", "goal", "goalRatePctWeek", "targetWeightKg", "bodyFatPct", "trainingDaysWeek", "sessionMinutes"),
                    listOf(listOf(p.sex.dbValue, p.birthDate, p.heightCm, p.experience.dbValue, p.activityPal, p.goal.dbValue, p.goalRatePctWeek, p.targetWeightKg, p.bodyFatPct, p.trainingDaysWeek, p.sessionMinutes)),
                )
            }
            zip.writeCsv(
                "body_measurements.csv",
                listOf("measuredOn", "weightKg", "bodyFatPct", "waistCm", "hipCm", "chestCm", "armCm", "thighCm", "source"),
                data.bodyMeasurements.map { listOf(it.measuredOn, it.weightKg, it.bodyFatPct, it.waistCm, it.hipCm, it.chestCm, it.armCm, it.thighCm, it.source) },
            )
            zip.writeCsv(
                "food_entries.csv",
                listOf("loggedDate", "slot", "foodId", "recipeId", "grams", "method", "kcal", "proteinG", "fatG", "carbsG"),
                data.foodEntries.map { listOf(it.loggedDate, it.slot.dbValue, it.foodId, it.recipeId, it.grams, it.method.dbValue, it.kcal, it.proteinG, it.fatG, it.carbsG) },
            )
            zip.writeCsv(
                "water_entries.csv",
                listOf("loggedDate", "amountMl"),
                data.waterEntries.map { listOf(it.loggedDate, it.amountMl) },
            )
            zip.writeCsv(
                "workout_sessions.csv",
                listOf("id", "programDayId", "startedAt", "finishedAt", "perceivedEffort", "notes"),
                data.workoutSessions.map { (s, _) -> listOf(s.id, s.programDayId, s.startedAt, s.finishedAt, s.perceivedEffort, s.notes) },
            )
            zip.writeCsv(
                "workout_sets.csv",
                listOf("sessionId", "exerciseId", "setIndex", "weightKg", "reps", "durationSec", "distanceM", "rir", "isWarmup", "completed", "loggedAt"),
                data.workoutSessions.flatMap { (s, sets) -> sets.map { listOf(s.id, it.exerciseId, it.setIndex, it.weightKg, it.reps, it.durationSec, it.distanceM, it.rir, it.isWarmup, it.completed, it.loggedAt) } },
            )
            zip.writeCsv(
                "programs.csv",
                listOf("id", "name", "goal", "daysPerWeek", "weeks", "splitType", "generatedBy", "isActive"),
                data.programs.map { listOf(it.id, it.name, it.goal.dbValue, it.daysPerWeek, it.weeks, it.splitType, it.generatedBy, it.isActive) },
            )
            zip.writeCsv(
                "program_items.csv",
                listOf("programId", "weekNumber", "dayIndex", "dayName", "exerciseId", "sortOrder", "sets", "repMin", "repMax", "durationMinutes", "targetRir", "restSeconds"),
                data.programs.flatMap { program ->
                    program.days.flatMap { day ->
                        day.items.map { item ->
                            listOf(program.id, day.weekNumber, day.dayIndex, day.name, item.exerciseId, item.sortOrder, item.sets, item.repMin, item.repMax, item.durationMinutes, item.targetRir, item.restSeconds)
                        }
                    }
                },
            )
        }
        return out.toByteArray()
    }

    private fun ZipOutputStream.writeCsv(name: String, header: List<String>, rows: List<List<Any?>>) {
        putNextEntry(ZipEntry(name))
        write(CsvWriter.write(header, rows).toByteArray(Charsets.UTF_8))
        closeEntry()
    }
}
