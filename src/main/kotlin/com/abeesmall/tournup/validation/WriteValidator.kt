package com.abeesmall.tournup.validation

import jakarta.validation.ConstraintViolationException
import jakarta.validation.Validator

object WriteValidator {
    fun validate(validator: Validator, request: Any) {
        val violations = validator.validate(request)
        if (violations.isNotEmpty()) throw ConstraintViolationException(violations)
    }
}
