package com.abeesmall.tournup.dto.audit

import java.util.UUID

interface AuditedResource<T> {
    val id: String
    @get:com.fasterxml.jackson.annotation.JsonIgnore
    val internalId: UUID
    fun withLastAudit(value: LastAuditValue): T
}
