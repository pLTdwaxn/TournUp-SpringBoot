package com.abeesmall.tournup.controller

import com.abeesmall.tournup.controller.season.SeasonController
import com.abeesmall.tournup.controller.tournament.TournamentController
import com.abeesmall.tournup.service.RecordContextRequiredException
import com.abeesmall.tournup.service.season.SeasonFieldConflictException
import com.abeesmall.tournup.service.season.SeasonTournamentChangeException
import com.abeesmall.tournup.service.season.SeasonTournamentNotFoundException
import com.abeesmall.tournup.service.tournament.TournamentSlugConflictException
import com.abeesmall.tournup.ui.resource.UiResourceRegistry
import jakarta.servlet.http.HttpServletRequest
import jakarta.validation.ConstraintViolationException
import org.springframework.beans.factory.ObjectProvider
import org.springframework.http.HttpStatus
import org.springframework.http.ProblemDetail
import org.springframework.http.converter.HttpMessageNotReadableException
import org.springframework.web.bind.MethodArgumentNotValidException
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.RestControllerAdvice

@RestControllerAdvice(assignableTypes = [SeasonController::class, TournamentController::class])
class ResourceExceptionHandler(private val registry: ObjectProvider<UiResourceRegistry>) {
    @ExceptionHandler(MethodArgumentNotValidException::class)
    fun invalidRequest(exception: MethodArgumentNotValidException, request: HttpServletRequest): ProblemDetail =
        validationProblem(
            exception.bindingResult.fieldErrors.groupBy { it.field }.mapValues { (_, errors) ->
                errors.map { it.defaultMessage ?: "Invalid value" }.distinct()
            }, request,
        )

    @ExceptionHandler(ConstraintViolationException::class)
    fun invalidServiceRequest(exception: ConstraintViolationException, request: HttpServletRequest): ProblemDetail =
        validationProblem(
            exception.constraintViolations.groupBy { it.propertyPath.toString() }
                .mapValues { (_, violations) -> violations.map { it.message }.distinct() }, request,
        )

    @ExceptionHandler(HttpMessageNotReadableException::class)
    fun invalidBody(): ProblemDetail = ProblemDetail.forStatusAndDetail(
        HttpStatus.BAD_REQUEST, "Request body is missing or contains invalid field types or values",
    )

    @ExceptionHandler(SeasonTournamentChangeException::class)
    fun tournamentChange(exception: SeasonTournamentChangeException, request: HttpServletRequest): ProblemDetail =
        validationProblem(mapOf("tournamentId" to listOf(exception.message.orEmpty())), request)

    @ExceptionHandler(RecordContextRequiredException::class)
    fun missingContext(exception: RecordContextRequiredException): ProblemDetail =
        ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, exception.message.orEmpty())

    @ExceptionHandler(SeasonFieldConflictException::class)
    fun seasonConflict(exception: SeasonFieldConflictException): ProblemDetail =
        conflict(exception.field, exception.message ?: "Season already exists")

    @ExceptionHandler(TournamentSlugConflictException::class)
    fun tournamentConflict(exception: TournamentSlugConflictException): ProblemDetail =
        conflict("slug", exception.message ?: "Tournament slug already exists")

    @ExceptionHandler(SeasonTournamentNotFoundException::class)
    fun missingTournament(): ProblemDetail =
        ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, "Tournament not found")

    private fun validationProblem(errors: Map<String, List<String>>, request: HttpServletRequest): ProblemDetail {
        val resources = registry.ifAvailable?.resources.orEmpty()
        val resource = request.requestURI.split('/').mapNotNull { resources.values.firstOrNull { resource -> resource.pluralName == it }?.key }
            .lastOrNull() ?: "resource"
        return ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, "Invalid $resource data").apply {
            setProperty("errors", errors)
        }
    }

    private fun conflict(field: String, message: String): ProblemDetail =
        ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, message).apply {
            setProperty("errors", mapOf(field to listOf(message)))
        }

    // TODO: Give future resources domain-owned problem translators so this advice stays generic.
}
