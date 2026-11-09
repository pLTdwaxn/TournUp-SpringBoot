package com.abeesmall.tournup.dto.audit

import com.fasterxml.jackson.annotation.JsonValue

data class LastAuditValue(
    @get:JsonValue
    val value: LastAuditData?,
)
