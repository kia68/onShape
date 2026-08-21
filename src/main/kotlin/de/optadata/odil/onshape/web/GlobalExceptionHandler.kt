package de.optadata.odil.onshape.web

import de.optadata.odil.onshape.auth.EmailAlreadyRegisteredException
import de.optadata.odil.onshape.auth.InvalidCredentialsException
import de.optadata.odil.onshape.onboarding.GoalRateExceededException
import de.optadata.odil.onshape.onboarding.TargetWeightBmiTooLowException
import de.optadata.odil.onshape.onboarding.UnderMinimumAgeException
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

    @ExceptionHandler(MethodArgumentNotValidException::class)
    fun handleValidation(e: MethodArgumentNotValidException): ResponseEntity<ApiError> {
        val fieldErrors = e.bindingResult.fieldErrors.associate { it.field to (it.defaultMessage ?: "invalid") }
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(ApiError("validation_failed", "Eingabe ungueltig", fieldErrors))
    }
}
