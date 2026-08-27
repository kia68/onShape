package de.optadata.odil.onshape.web

import de.optadata.odil.onshape.auth.EmailAlreadyRegisteredException
import de.optadata.odil.onshape.auth.InvalidCredentialsException
import de.optadata.odil.onshape.billing.BillingNotConfiguredException
import de.optadata.odil.onshape.billing.InvalidWebhookSignatureException
import de.optadata.odil.onshape.billing.LifetimeCapReachedException
import de.optadata.odil.onshape.billing.NoStripeCustomerException
import de.optadata.odil.onshape.legal.CoreConsentImmutableException
import de.optadata.odil.onshape.legal.CoreConsentRequiredException
import de.optadata.odil.onshape.nutrition.InvalidRecipeUrlException
import de.optadata.odil.onshape.nutrition.RecipeImportFailedException
import de.optadata.odil.onshape.onboarding.AdaptiveTdeeRequiresUpgradeException
import de.optadata.odil.onshape.onboarding.GoalRateExceededException
import de.optadata.odil.onshape.onboarding.TargetWeightBmiTooLowException
import de.optadata.odil.onshape.onboarding.UnderMinimumAgeException
import de.optadata.odil.onshape.progress.WeeklyReportRequiresUpgradeException
import de.optadata.odil.onshape.training.ProgramLimitExceededException
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.MethodArgumentNotValidException
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.RestControllerAdvice

@RestControllerAdvice
class GlobalExceptionHandler {

    @ExceptionHandler(EmailAlreadyRegisteredException::class)
    fun handleEmailTaken(e: EmailAlreadyRegisteredException) =
        ResponseEntity.status(HttpStatus.CONFLICT).body(ApiError("email_taken", e.message ?: "email taken"))

    @ExceptionHandler(InvalidCredentialsException::class)
    fun handleInvalidCredentials(e: InvalidCredentialsException) =
        ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(ApiError("invalid_credentials", e.message ?: "invalid credentials"))

    @ExceptionHandler(GoalRateExceededException::class)
    fun handleGoalRateExceeded(e: GoalRateExceededException) =
        ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY).body(ApiError("goal_rate_exceeded", e.message ?: "goal rate exceeded"))

    @ExceptionHandler(UnderMinimumAgeException::class)
    fun handleUnderMinimumAge(e: UnderMinimumAgeException) =
        ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY).body(ApiError("under_minimum_age", e.message ?: "under minimum age"))

    @ExceptionHandler(TargetWeightBmiTooLowException::class)
    fun handleTargetWeightBmiTooLow(e: TargetWeightBmiTooLowException) =
        ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY).body(ApiError("target_weight_bmi_too_low", e.message ?: "target weight bmi too low"))

    @ExceptionHandler(CoreConsentRequiredException::class)
    fun handleCoreConsentRequired(e: CoreConsentRequiredException) =
        ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY).body(ApiError("core_consent_required", e.message ?: "core consent required"))

    @ExceptionHandler(CoreConsentImmutableException::class)
    fun handleCoreConsentImmutable(e: CoreConsentImmutableException) =
        ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY).body(ApiError("core_consent_immutable", e.message ?: "core consent immutable"))

    @ExceptionHandler(ProgramLimitExceededException::class)
    fun handleProgramLimitExceeded(e: ProgramLimitExceededException) =
        ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY).body(ApiError("program_limit_exceeded", e.message ?: "program limit exceeded"))

    @ExceptionHandler(InvalidRecipeUrlException::class)
    fun handleInvalidRecipeUrl(e: InvalidRecipeUrlException) =
        ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY).body(ApiError("invalid_recipe_url", e.message ?: "invalid recipe url"))

    @ExceptionHandler(RecipeImportFailedException::class)
    fun handleRecipeImportFailed(e: RecipeImportFailedException) =
        ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY).body(ApiError("recipe_import_failed", e.message ?: "recipe import failed"))

    @ExceptionHandler(AdaptiveTdeeRequiresUpgradeException::class)
    fun handleAdaptiveTdeeRequiresUpgrade(e: AdaptiveTdeeRequiresUpgradeException) =
        ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY).body(ApiError("adaptive_tdee_requires_upgrade", e.message ?: "adaptive tdee requires upgrade"))

    @ExceptionHandler(WeeklyReportRequiresUpgradeException::class)
    fun handleWeeklyReportRequiresUpgrade(e: WeeklyReportRequiresUpgradeException) =
        ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY).body(ApiError("weekly_report_requires_upgrade", e.message ?: "weekly report requires upgrade"))

    @ExceptionHandler(BillingNotConfiguredException::class)
    fun handleBillingNotConfigured(e: BillingNotConfiguredException) =
        ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(ApiError("billing_not_configured", e.message ?: "billing not configured"))

    @ExceptionHandler(LifetimeCapReachedException::class)
    fun handleLifetimeCapReached(e: LifetimeCapReachedException) =
        ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY).body(ApiError("lifetime_cap_reached", e.message ?: "lifetime cap reached"))

    @ExceptionHandler(NoStripeCustomerException::class)
    fun handleNoStripeCustomer(e: NoStripeCustomerException) =
        ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY).body(ApiError("no_stripe_customer", e.message ?: "no stripe customer"))

    @ExceptionHandler(InvalidWebhookSignatureException::class)
    fun handleInvalidWebhookSignature(e: InvalidWebhookSignatureException) =
        ResponseEntity.status(HttpStatus.BAD_REQUEST).body(ApiError("invalid_webhook_signature", e.message ?: "invalid webhook signature"))

    @ExceptionHandler(MethodArgumentNotValidException::class)
    fun handleValidation(e: MethodArgumentNotValidException): ResponseEntity<ApiError> {
        val fieldErrors = e.bindingResult.fieldErrors.associate { it.field to (it.defaultMessage ?: "invalid") }
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(ApiError("validation_failed", "Eingabe ungueltig", fieldErrors))
    }
}
