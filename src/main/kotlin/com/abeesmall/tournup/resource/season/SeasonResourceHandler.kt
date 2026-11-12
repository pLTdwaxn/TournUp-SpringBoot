package com.abeesmall.tournup.resource.season

import com.abeesmall.tournup.dto.season.SeasonData
import com.abeesmall.tournup.dto.season.SeasonWriteRequest
import com.abeesmall.tournup.manifest.season.seasonResourceDefinition
import com.abeesmall.tournup.model.season.Season
import com.abeesmall.tournup.resource.ResourceHandler
import com.abeesmall.tournup.resource.ResourceRequest
import com.abeesmall.tournup.service.resourcePage
import com.abeesmall.tournup.service.season.SeasonService
import com.abeesmall.tournup.ui.ResourceResponseComposer
import com.abeesmall.tournup.ui.UiOperation
import jakarta.validation.Validator
import java.util.UUID
import org.springframework.data.domain.Sort
import org.springframework.stereotype.Component
import tools.jackson.databind.json.JsonMapper

@Component
class SeasonResourceHandler(
    private val service: SeasonService, mapper: JsonMapper, validator: Validator, responses: ResourceResponseComposer,
) : ResourceHandler<SeasonWriteRequest, SeasonWriteRequest, SeasonData>(
    seasonResourceDefinition(), seasonCreateContract, seasonUpdateContract, mapper, validator, responses, Season::class.java,
) {
    override fun title(item: SeasonData) = item.name
    override fun application(context: ResourceRequest, operation: UiOperation) =
        if (operation == UiOperation.CREATE) context.requireApplication() else super.application(context, operation)
    override fun createFormData(context: ResourceRequest) = service.createForm(context.uuid("tournamentId"))
    override fun editFormData(id: UUID, context: ResourceRequest) = service.editForm(id, context.parentIdentifier?.let(UUID::fromString))
    override fun relatedIds(context: ResourceRequest) = context.parentIdentifier?.let { mapOf("tournamentId" to it) }.orEmpty()
    override fun listRecords(context: ResourceRequest) = service.list(
        context.parentIdentifier?.let(UUID::fromString),
        resourcePage(context.page, context.size,
            Sort.by(Sort.Order.desc("fromYear"), Sort.Order.asc("name"), Sort.Order.asc("id"))),
    )
    override fun findRecord(identifier: String, context: ResourceRequest) = service.find(identifier, context.parentIdentifier)
    override fun createRecord(request: SeasonWriteRequest, context: ResourceRequest) = service.create(request)
    override fun updateRecord(id: UUID, request: SeasonWriteRequest, context: ResourceRequest) =
        service.update(id, request, context.parentIdentifier?.let(UUID::fromString))
}
