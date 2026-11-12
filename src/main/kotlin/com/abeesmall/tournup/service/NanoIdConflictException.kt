package com.abeesmall.tournup.service

import org.hibernate.exception.ConstraintViolationException
import org.springframework.dao.DataIntegrityViolationException

class NanoIdConflictException(cause: Throwable) :
    RuntimeException("NanoID is already in use. Reload the create form and try again.", cause)

internal fun nanoIdConflict(failure: DataIntegrityViolationException): NanoIdConflictException? =
    generateSequence<Throwable>(failure) { it.cause }
        .filterIsInstance<ConstraintViolationException>()
        .firstOrNull { it.constraintName in setOf("tournaments_nano_id_key", "seasons_nano_id_key") }
        ?.let { NanoIdConflictException(failure) }
