package com.abeesmall.tournup.auth

import org.springframework.http.HttpStatus
import org.springframework.http.ProblemDetail
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.RestControllerAdvice

@RestControllerAdvice(assignableTypes = [AuthController::class])
class AuthExceptionHandler {

    @ExceptionHandler(InvalidAuthRequestException::class)
    fun invalidAuthRequest(
        exception: InvalidAuthRequestException,
    ): ProblemDetail =
        ProblemDetail.forStatusAndDetail(
            HttpStatus.BAD_REQUEST,
            exception.message ?: "Invalid authentication request",
        )
}
