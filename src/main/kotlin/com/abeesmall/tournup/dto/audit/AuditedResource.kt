package com.abeesmall.tournup.dto.audit

import java.util.UUID

interface AuditedResource<T> {
    val id: UUID
    fun withLastAudit(value: LastAuditValue): T
}
