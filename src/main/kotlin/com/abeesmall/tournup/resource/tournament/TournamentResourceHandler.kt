package com.abeesmall.tournup.resource.tournament

import com.abeesmall.tournup.dto.tournament.TournamentCreateRequest
import com.abeesmall.tournup.dto.tournament.TournamentData
import com.abeesmall.tournup.dto.tournament.TournamentUpdateRequest
import com.abeesmall.tournup.dto.ui.UiRecordResponse
import com.abeesmall.tournup.manifest.tournament.tournamentResourceDefinition
import com.abeesmall.tournup.model.tournament.Tournament
import com.abeesmall.tournup.resource.ResourceHandler
import com.abeesmall.tournup.resource.ResourceRequest
import com.abeesmall.tournup.service.resourcePage
import com.abeesmall.tournup.service.tournament.TournamentService
import com.abeesmall.tournup.ui.ResourceResponseComposer
import com.abeesmall.tournup.ui.UiOperation
import jakarta.validation.Validator
import java.net.URI
import java.util.UUID
import org.springframework.data.domain.Sort
import org.springframework.stereotype.Component
import tools.jackson.databind.json.JsonMapper

@Component
class TournamentResourceHandler(
    private val service: TournamentService, mapper: JsonMapper, validator: Validator, responses: ResourceResponseComposer,
) : ResourceHandler<TournamentCreateRequest, TournamentUpdateRequest, TournamentData>(
    tournamentResourceDefinition(), tournamentCreateContract, tournamentUpdateContract, mapper, validator, responses,
    Tournament::class.java,
) {
    override val listVariesByApplication = true
    override fun title(item: TournamentData) = item.name
    override fun application(context: ResourceRequest, operation: UiOperation) =
        if (operation == UiOperation.LIST) context.requireApplication() else super.application(context, operation)
    override fun editFormData(id: UUID, context: ResourceRequest) = service.editForm(id)
    override fun listRecords(context: ResourceRequest) =
        service.list(resourcePage(context.page, context.size, Sort.by("name", "id")))
    override fun findRecord(identifier: String, context: ResourceRequest) = service.find(identifier)
    override fun createRecord(request: TournamentCreateRequest, context: ResourceRequest) = service.create(request)
    override fun updateRecord(id: UUID, request: TournamentUpdateRequest, context: ResourceRequest) = service.update(id, request)
    override fun creationLocation(result: UiRecordResponse<TournamentData>): URI =
        URI.create("/api/${definition.pluralName}/${result.item.id}/")
}
