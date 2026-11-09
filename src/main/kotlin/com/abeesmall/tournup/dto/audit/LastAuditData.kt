package com.abeesmall.tournup.dto.audit

import java.time.Instant
import java.util.UUID

data class LastAuditData(
    val revision: Int,
    val timestamp: Instant,
    val actorId: UUID?,
)
