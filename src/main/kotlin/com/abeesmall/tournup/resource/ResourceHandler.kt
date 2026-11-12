package com.abeesmall.tournup.resource

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
import com.abeesmall.tournup.validation.WriteValidator
import jakarta.validation.Validator
import java.net.URI
import java.util.UUID
import org.springframework.data.domain.Page
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.core.Authentication
import org.springframework.web.server.ResponseStatusException
import tools.jackson.core.JacksonException
import tools.jackson.databind.JsonNode
import tools.jackson.databind.json.JsonMapper

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

class InvalidResourceBodyException(cause: Throwable? = null) : RuntimeException(cause)

/** Binds concrete DTOs and composes responses after service transactions return. */
abstract class ResourceHandler<C : Any, U : Any, R : AuditedResource<R>>(
    private val registration: UiResourceRegistration,
    private val createContract: WriteContract<C>,
    private val updateContract: WriteContract<U>,
    private val mapper: JsonMapper,
    private val validator: Validator,
    private val responses: ResourceResponseComposer,
    private val entityClass: Class<*>,
) {
    val definition: ResourceDefinition get() = registration.definition
    init { require(definition.create === createContract && definition.update === updateContract) }
    open val listVariesByApplication = false

    fun list(context: ResourceRequest): UiCollectionResponse<R> {
        val manifest = manifestContext(context, UiOperation.LIST)
        return responses.collection(registration, listRecords(context), manifest, context.authentication, entityClass)
    }

    fun find(identifier: String, context: ResourceRequest): UiRecordResponse<R> =
        record(findRecord(identifier, context) ?: missing(), context)

    fun create(body: JsonNode, context: ResourceRequest): ResponseEntity<UiRecordResponse<R>> {
        val result = record(createRecord(bind(body, createContract), context), context)
        val response = ResponseEntity.status(HttpStatus.CREATED)
        creationLocation(result)?.let { response.location(it) }
        return response.body(result)
    }

    fun update(id: UUID, body: JsonNode, context: ResourceRequest): UiRecordResponse<R> =
        updateTyped(id, bind(body, updateContract), context)

    fun updateTyped(id: UUID, request: U, context: ResourceRequest): UiRecordResponse<R> =
        record(updateRecord(id, request, context) ?: missing(), context)

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
            allowedWrites = if (definition.canWrite(request.authentication)) {
                setOf(UiOperation.CREATE, UiOperation.UPDATE)
            } else emptySet(), title = title, recordId = recordId, relatedIds = relatedIds(request))

    private fun <T : Any> bind(body: JsonNode, contract: WriteContract<T>): T {
        if (!body.isObject) throw InvalidResourceBodyException()
        val request = try { mapper.treeToValue(body, contract.requestType.java) }
            catch (failure: JacksonException) { throw InvalidResourceBodyException(failure) }
        WriteValidator.validate(validator, request)
        return request
    }

    protected fun missing(): Nothing = throw ResponseStatusException(
        HttpStatus.NOT_FOUND, "${definition.key.replaceFirstChar { it.uppercase() }} not found",
    )
    protected open fun application(context: ResourceRequest, operation: UiOperation) = UiApplication.TOURNAMENT_MANAGER
    protected open fun relatedIds(context: ResourceRequest): Map<String, String> = emptyMap()
    protected open fun creationLocation(result: UiRecordResponse<R>): URI? = null
    protected open fun createFormData(context: ResourceRequest): ResourceRecord<C> =
        ResourceRecord(requireNotNull(createContract.initialValues).invoke())
    protected abstract fun title(item: R): String
    protected abstract fun editFormData(id: UUID, context: ResourceRequest): ResourceRecord<U>?
    protected abstract fun listRecords(context: ResourceRequest): Page<R>
    protected abstract fun findRecord(identifier: String, context: ResourceRequest): ResourceRecord<R>?
    protected abstract fun createRecord(request: C, context: ResourceRequest): ResourceRecord<R>
    protected abstract fun updateRecord(id: UUID, request: U, context: ResourceRequest): ResourceRecord<R>?
}
