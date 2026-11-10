package com.abeesmall.tournup.service

import java.util.UUID

internal fun parseRecordId(value: String): UUID? =
    try {
        UUID.fromString(value)
    } catch (_: IllegalArgumentException) {
        null
    }

class RecordContextRequiredException(parentResource: String) :
    RuntimeException("$parentResource context is required for a slug lookup")
