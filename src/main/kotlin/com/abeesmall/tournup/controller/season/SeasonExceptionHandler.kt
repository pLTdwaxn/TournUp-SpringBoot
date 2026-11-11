package com.abeesmall.tournup.controller.season

import com.abeesmall.tournup.service.RecordContextRequiredException
import com.abeesmall.tournup.service.season.SeasonFieldConflictException
import com.abeesmall.tournup.service.season.SeasonTournamentChangeException
import com.abeesmall.tournup.service.season.SeasonTournamentNotFoundException
import jakarta.validation.ConstraintViolationException
import org.springframework.http.HttpStatus
import org.springframework.http.ProblemDetail
import org.springframework.http.converter.HttpMessageNotReadableException
import org.springframework.web.bind.MethodArgumentNotValidException
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.RestControllerAdvice

@RestControllerAdvice(
    assignableTypes = [
        SeasonCreateController::class,
        SeasonListController::class,
        SeasonUpdateController::class,
        SeasonDetailController::class,
    ],
)
class SeasonExceptionHandler {
    @ExceptionHandler(SeasonTournamentChangeException::class)
    fun tournamentChange(exception: SeasonTournamentChangeException): ProblemDetail =
        validationProblem(mapOf("tournamentId" to listOf(exception.message.orEmpty())))

    @ExceptionHandler(RecordContextRequiredException::class)
    fun missingContext(exception: RecordContextRequiredException): ProblemDetail =
        ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, exception.message.orEmpty())

    @ExceptionHandler(MethodArgumentNotValidException::class)
    fun invalidRequest(exception: MethodArgumentNotValidException): ProblemDetail =
        validationProblem(
            exception.bindingResult.fieldErrors.groupBy { it.field }
                .mapValues { (_, errors) ->
                    errors.map { it.defaultMessage ?: "Invalid value" }.distinct()
                },
        )

    @ExceptionHandler(ConstraintViolationException::class)
    fun invalidServiceRequest(exception: ConstraintViolationException): ProblemDetail =
        validationProblem(
            exception.constraintViolations.groupBy { it.propertyPath.toString() }
                .mapValues { (_, violations) -> violations.map { it.message }.distinct() },
        )

    @ExceptionHandler(SeasonFieldConflictException::class)
    fun conflict(exception: SeasonFieldConflictException): ProblemDetail {
        val message = exception.message ?: "Season already exists"
        return ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, message).apply {
            setProperty("errors", mapOf(exception.field to listOf(message)))
        }
    }

    @ExceptionHandler(SeasonTournamentNotFoundException::class)
    fun missingTournament(): ProblemDetail =
        ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, "Tournament not found")

    @ExceptionHandler(HttpMessageNotReadableException::class)
    fun invalidBody(): ProblemDetail =
        ProblemDetail.forStatusAndDetail(
            HttpStatus.BAD_REQUEST,
            "Request body is missing or contains invalid field types or values",
        )

    private fun validationProblem(errors: Map<String, List<String>>): ProblemDetail =
        ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, "Invalid season data").apply {
            setProperty("errors", errors)
        }
}
