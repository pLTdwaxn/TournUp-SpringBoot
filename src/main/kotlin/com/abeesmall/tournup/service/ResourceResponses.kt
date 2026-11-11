package com.abeesmall.tournup.service

import com.abeesmall.tournup.dto.audit.AuditedResource
import com.abeesmall.tournup.dto.audit.LastAuditValue
import com.abeesmall.tournup.dto.ui.UiCollectionResponse
import com.abeesmall.tournup.dto.ui.UiPagination
import com.abeesmall.tournup.dto.ui.UiRecordResponse
import com.abeesmall.tournup.repository.audit.LatestAuditReader
import com.abeesmall.tournup.security.canViewLastAudit
import com.abeesmall.tournup.ui.UiManifest
import org.springframework.data.domain.Page
import org.springframework.security.core.Authentication

fun <T : Any, R> Page<T>.toUiResponse(manifest: UiManifest, transform: (T) -> R): UiCollectionResponse<R> =
    UiCollectionResponse(manifest, content.map(transform), UiPagination(number, size, totalElements, totalPages))

// Call after a write service returns: Envers creates its revision at transaction commit.
fun <T : AuditedResource<T>> UiRecordResponse<T>.withLatestAudit(
    entityClass: Class<*>, authentication: Authentication, reader: LatestAuditReader,
): UiRecordResponse<T> {
    if (!canViewLastAudit(authentication)) return this
    val audit = reader.findLatest(entityClass, listOf(item.id))[item.id]
    return copy(item = item.withLastAudit(LastAuditValue(audit)))
}

fun <T : AuditedResource<T>> UiCollectionResponse<T>.withLatestAudit(
    entityClass: Class<*>, authentication: Authentication, reader: LatestAuditReader,
): UiCollectionResponse<T> {
    if (!canViewLastAudit(authentication) || items.isEmpty()) return this
    val audits = reader.findLatest(entityClass, items.map { it.id })
    return copy(items = items.map { it.withLastAudit(LastAuditValue(audits[it.id])) })
}
