package com.abeesmall.tournup.service

import com.abeesmall.tournup.dto.audit.AuditedResource
import com.abeesmall.tournup.dto.ui.UiCollectionResponse
import com.abeesmall.tournup.dto.ui.UiRecordResponse
import com.abeesmall.tournup.service.ResourceRecord
import com.abeesmall.tournup.ui.ResourceResponseComposer
import com.abeesmall.tournup.ui.UI_APPLICATION_HEADER
import com.abeesmall.tournup.ui.UiApplication
import com.abeesmall.tournup.ui.UiManifestContext
import com.abeesmall.tournup.ui.UiOperation
import com.abeesmall.tournup.ui.resource.UiResourceRegistration
import java.net.URI
import java.util.UUID
import org.springframework.data.domain.Page
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.core.Authentication
import org.springframework.web.server.ResponseStatusException

data class ResourceRequest(
    val authentication: Authentication,
    val application: String? = null,
    val query: Map<String, String> = emptyMap(),
    val page: Int = 0,
    val size: Int = 20,
    val parentIdentifier: String? = null,
) {
    fun uuid(name: String): UUID? = query[name]?.takeIf { it.isNotEmpty() }?.let {
        try { UUID.fromString(it) } catch (_: IllegalArgumentException) {
            throw ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid $name")
        }
    }

    fun requireApplication(): UiApplication = UiApplication.fromHeader(application ?: throw ResponseStatusException(
        HttpStatus.BAD_REQUEST, "Missing $UI_APPLICATION_HEADER header",
    )) ?: throw ResponseStatusException(HttpStatus.BAD_REQUEST, "Unknown $UI_APPLICATION_HEADER value")
}

/** Composes typed CRUD operations and responses; persistence callbacks retain their service transactions. */
class CrudDelegate<C : Any, U : Any, R : AuditedResource<R>>(
    private val registration: UiResourceRegistration,
    private val responses: ResourceResponseComposer,
    private val entityClass: Class<*>,
    private val title: (R) -> String,
    private val listRecords: (ResourceRequest) -> Page<R>,
    private val findRecord: (String, ResourceRequest) -> ResourceRecord<R>?,
    private val createRecord: (C, ResourceRequest) -> ResourceRecord<R>,
    private val updateRecord: (UUID, U, ResourceRequest) -> ResourceRecord<R>?,
    private val editFormData: (UUID, ResourceRequest) -> ResourceRecord<U>?,
    private val createFormData: (ResourceRequest) -> ResourceRecord<C>,
    private val application: (ResourceRequest, UiOperation) -> UiApplication = { _, _ -> UiApplication.TOURNAMENT_MANAGER },
    private val relatedIds: (ResourceRequest) -> Map<String, String> = { emptyMap() },
    private val creationLocation: (UiRecordResponse<R>) -> URI? = { null },
) {

    fun list(context: ResourceRequest): UiCollectionResponse<R> {
        val manifest = manifestContext(context, UiOperation.LIST)
        return responses.collection(registration, listRecords(context), manifest, context.authentication, entityClass)
    }

    fun find(identifier: String, context: ResourceRequest): UiRecordResponse<R> =
        record(findRecord(identifier, context) ?: missing(), context)

    fun create(input: C, context: ResourceRequest): ResponseEntity<UiRecordResponse<R>> {
        val result = record(createRecord(input, context), context)
        val response = ResponseEntity.status(HttpStatus.CREATED)
        creationLocation(result)?.let { response.location(it) }
        return response.body(result)
    }

    fun update(id: UUID, input: U, context: ResourceRequest): UiRecordResponse<R> {
        return record(updateRecord(id, input, context) ?: missing(), context)
    }

    fun createForm(context: ResourceRequest): UiRecordResponse<C> {
        val manifest = manifestContext(context, UiOperation.CREATE)
        return responses.form(registration, createFormData(context), manifest)
    }

    fun editForm(id: UUID, context: ResourceRequest): UiRecordResponse<U> = responses.form(
        registration, editFormData(id, context) ?: missing(), manifestContext(context, UiOperation.UPDATE, recordId = id.toString()),
    )

    private fun record(result: ResourceRecord<R>, request: ResourceRequest): UiRecordResponse<R> =
        responses.record(registration, result,
            manifestContext(request, UiOperation.READ, title(result.item), result.item.id.toString()),
            request.authentication, entityClass)

    private fun manifestContext(request: ResourceRequest, operation: UiOperation, title: String? = null, recordId: String? = null) =
        UiManifestContext(application(request, operation), operation,
            allowedWrites = if (registration.canWrite(request.authentication)) {
                setOf(UiOperation.CREATE, UiOperation.UPDATE)
            } else emptySet(), title = title, recordId = recordId, relatedIds = relatedIds(request))

    private fun missing(): Nothing = throw ResponseStatusException(HttpStatus.NOT_FOUND, "${registration.key.replaceFirstChar { it.uppercase() }} not found")
}
