package com.abeesmall.tournup.ui

import com.abeesmall.tournup.dto.audit.AuditedResource
import com.abeesmall.tournup.dto.audit.LastAuditValue
import com.abeesmall.tournup.dto.ui.UiCollectionResponse
import com.abeesmall.tournup.dto.ui.UiPagination
import com.abeesmall.tournup.dto.ui.UiRecordResponse
import com.abeesmall.tournup.repository.audit.LatestAuditReader
import com.abeesmall.tournup.security.canViewLastAudit
import com.abeesmall.tournup.service.ResourceRecord
import com.abeesmall.tournup.ui.resource.UiResourceRegistration
import org.springframework.data.domain.Page
import org.springframework.security.core.Authentication
import org.springframework.stereotype.Component

@Component
class ResourceResponseComposer(
    private val manifests: UiManifestResolver,
    private val audits: LatestAuditReader,
) {
    fun <T> form(definition: UiResourceRegistration, record: ResourceRecord<T>, context: UiManifestContext) =
        UiRecordResponse(manifests.resolve(definition, context), record.item, record.included)

    fun <T : AuditedResource<T>> record(
        definition: UiResourceRegistration, record: ResourceRecord<T>, context: UiManifestContext,
        authentication: Authentication, entityClass: Class<*>,
    ): UiRecordResponse<T> = form(definition, record, context).withLatestAudit(entityClass, authentication, audits)

    fun <T : AuditedResource<T>> collection(
        definition: UiResourceRegistration, page: Page<T>, context: UiManifestContext,
        authentication: Authentication, entityClass: Class<*>,
    ): UiCollectionResponse<T> = UiCollectionResponse(
        manifests.resolve(definition, context), page.content,
        UiPagination(page.number, page.size, page.totalElements, page.totalPages),
    ).withLatestAudit(entityClass, authentication, audits)
}

// Call after a write service returns: Envers creates its revision at transaction commit.
fun <T : AuditedResource<T>> UiRecordResponse<T>.withLatestAudit(
    entityClass: Class<*>, authentication: Authentication, reader: LatestAuditReader,
): UiRecordResponse<T> {
    if (!canViewLastAudit(authentication)) return this
    val audit = reader.findLatest(entityClass, listOf(item.internalId))[item.internalId]
    return copy(item = item.withLastAudit(LastAuditValue(audit)))
}

fun <T : AuditedResource<T>> UiCollectionResponse<T>.withLatestAudit(
    entityClass: Class<*>, authentication: Authentication, reader: LatestAuditReader,
): UiCollectionResponse<T> {
    if (!canViewLastAudit(authentication) || items.isEmpty()) return this
    val audits = reader.findLatest(entityClass, items.map { it.internalId })
    return copy(items = items.map { it.withLastAudit(LastAuditValue(audits[it.internalId])) })
}
