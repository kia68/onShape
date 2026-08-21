package de.optadata.odil.onshape.onboarding

import de.optadata.odil.onshape.security.RlsSession
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Service
import org.springframework.web.server.ResponseStatusException
import java.time.Clock
import java.time.LocalDate
import java.util.UUID

/**
 * Orchestriert FR-02..FR-11 als einen kombinierten Schritt. Die PAR-Q+-Antworten (FR-07)
 * werden bewusst NICHT persistiert -- es gibt dafuer keine Tabelle (siehe V1-V8) und der Zweck
 * laut KONZEPT.md ist ein Live-Hinweis, kein medizinisches Datenarchiv. Das minimiert die
 * Menge an gespeicherten Gesundheitsdaten (Art. 9 DSGVO), bis die Recht/Compliance-Epic (#12)
 * eine Rechtsgrundlage dafuer klaert.
 */
@Service
class OnboardingService(
    private val profileRepository: ProfileRepository,
    private val bodyMeasurementRepository: BodyMeasurementRepository,
    private val nutritionTargetRepository: NutritionTargetRepository,
    private val rlsSession: RlsSession,
    private val clock: Clock = Clock.systemUTC(),
) {

    fun submit(userId: UUID, request: OnboardingRequest): OnboardingResultResponse {
        val sex = parseEnum(Sex.entries, request.sex, "sex")
        val experience = parseEnum(Experience.entries, request.experience, "experience")
        val goal = parseEnum(Goal.entries, request.goal, "goal")
        val today = LocalDate.now(clock)

        SafetyLimits.requireMinimumAge(request.birthDate, today)
        SafetyLimits.requireHealthyTargetBmi(request.targetWeightKg, request.heightCm)
        GoalRateValidator.validate(goal, request.goalRatePctWeek)

        val profile = Profile(
            userId = userId,
            sex = sex,
            birthDate = request.birthDate,
            heightCm = request.heightCm,
            experience = experience,
            activityPal = request.activityPal,
            goal = goal,
            goalRatePctWeek = request.goalRatePctWeek,
            targetWeightKg = request.targetWeightKg,
            bodyFatPct = request.bodyFatPct,
            dietaryPrefs = request.dietaryPrefs,
            allergens = request.allergens,
            injuries = request.injuries,
            equipment = request.equipment,
            trainingDaysWeek = request.trainingDaysWeek,
            sessionMinutes = request.sessionMinutes,
        )

        val calculationInput = NutritionTargetInput(
            sex = sex,
            birthDate = request.birthDate,
            heightCm = request.heightCm,
            weightKg = request.weightKg,
            bodyFatPct = request.bodyFatPct,
            activityPal = request.activityPal,
            goal = goal,
            goalRatePctWeek = request.goalRatePctWeek,
            targetWeightKg = request.targetWeightKg,
            dietaryPrefs = request.dietaryPrefs,
        )
        val result = NutritionTargetCalculator.calculate(calculationInput, today)
        val healthAdvisory = HealthScreening.evaluate(request.healthScreening)

        rlsSession.asUser(userId) {
            profileRepository.upsert(profile)
            bodyMeasurementRepository.recordWeight(userId, today, request.weightKg, request.bodyFatPct)
            nutritionTargetRepository.insert(userId, today, result)
        }

        return result.toResponse(healthAdvisory)
    }

    fun latestResult(userId: UUID): OnboardingResultResponse? {
        val stored = rlsSession.asUser(userId) { nutritionTargetRepository.findLatest(userId) } ?: return null
        return stored.result.toResponse(HealthScreeningResult(needsMedicalAdvice = false, triggeredFlags = emptyList()))
    }

    private fun NutritionTargetResult.toResponse(healthAdvisory: HealthScreeningResult) = OnboardingResultResponse(
        kcal = kcal, proteinG = proteinG, fatG = fatG, carbsG = carbsG, fiberG = fiberG, waterMl = waterMl,
        calculation = calculation, healthAdvisory = healthAdvisory,
    )

    private fun <E : Enum<E>> parseEnum(values: List<E>, raw: String, field: String): E =
        values.firstOrNull { (it as? EnumWithDbValue)?.dbValue == raw }
            ?: throw ResponseStatusException(HttpStatus.BAD_REQUEST, "Ungueltiger Wert fuer $field: $raw")
}
