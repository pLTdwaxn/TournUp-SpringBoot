package com.abeesmall.tournup.tournament

import org.springframework.http.HttpStatus
import org.springframework.http.ProblemDetail
import org.springframework.http.converter.HttpMessageNotReadableException
import org.springframework.web.bind.MethodArgumentNotValidException
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.RestControllerAdvice

@RestControllerAdvice(
    assignableTypes = [
        TournamentCreateController::class,
        TournamentUpdateController::class,
    ],
)
class TournamentWriteExceptionHandler {
    @ExceptionHandler(MethodArgumentNotValidException::class)
    fun invalidRequest(
        exception: MethodArgumentNotValidException,
    ): ProblemDetail =
        ProblemDetail.forStatusAndDetail(
            HttpStatus.BAD_REQUEST,
            "Invalid tournament data",
        ).apply {
            setProperty(
                "errors",
                exception.bindingResult.fieldErrors
                    .groupBy { it.field }
                    .mapValues { (_, errors) ->
                        errors.map { it.defaultMessage ?: "Invalid value" }
                            .distinct()
                    },
            )
        }

    @ExceptionHandler(TournamentSlugConflictException::class)
    fun slugConflict(
        exception: TournamentSlugConflictException,
    ): ProblemDetail {
        val message = exception.message ?: "Tournament slug already exists"

        return ProblemDetail.forStatusAndDetail(
            HttpStatus.CONFLICT,
            message,
        ).apply {
            setProperty("errors", mapOf("slug" to listOf(message)))
        }
    }

    @ExceptionHandler(HttpMessageNotReadableException::class)
    fun invalidBody(): ProblemDetail =
        ProblemDetail.forStatusAndDetail(
            HttpStatus.BAD_REQUEST,
            "Request body is missing or contains invalid field types or values",
        )
}
