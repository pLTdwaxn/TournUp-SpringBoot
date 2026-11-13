package com.abeesmall.tournup.controller.season

import com.abeesmall.tournup.dto.season.SeasonData
import com.abeesmall.tournup.dto.season.SeasonWriteRequest
import com.abeesmall.tournup.dto.ui.UiCollectionResponse
import com.abeesmall.tournup.dto.ui.UiRecordResponse
import com.abeesmall.tournup.manifest.seasonResourceDefinition
import com.abeesmall.tournup.model.season.Season
import com.abeesmall.tournup.security.ResourceAccess
import com.abeesmall.tournup.service.CrudDelegate
import com.abeesmall.tournup.service.ResourceRequest
import com.abeesmall.tournup.service.resourcePage
import com.abeesmall.tournup.service.season.SeasonService
import com.abeesmall.tournup.ui.ResourceResponseComposer
import com.abeesmall.tournup.ui.UI_APPLICATION_HEADER
import com.abeesmall.tournup.ui.UiApplication
import com.abeesmall.tournup.ui.UiOperation
import com.abeesmall.tournup.ui.resource.ResourceAction
import jakarta.servlet.http.HttpServletResponse
import com.abeesmall.tournup.validation.ResourceLocator
import jakarta.validation.Valid
import org.springframework.data.domain.Sort
import org.springframework.http.ResponseEntity
import org.springframework.security.core.Authentication
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestHeader
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@RestController
class SeasonController(private val service: SeasonService, responses: ResourceResponseComposer) {
    private val crud = CrudDelegate<SeasonWriteRequest, SeasonWriteRequest, SeasonData>(
        seasonResourceDefinition(), responses, Season::class.java,
        title = { it.name },
        listRecords = {
            service.list(
                it.parentIdentifier,
                resourcePage(
                    it.page,
                    it.size,
                    Sort.by(Sort.Order.desc("fromYear"), Sort.Order.asc("name"), Sort.Order.asc("id"))
                )
            )
        },
        findRecord = { id, context -> service.find(id, context.parentIdentifier) },
        createRecord = { input, _ -> service.create(input) },
        updateRecord = { id, input, context -> service.update(id, input, context.parentIdentifier) },
        editFormData = { id, context -> service.editForm(id, context.parentIdentifier) },
        createFormData = { service.createForm(it.query["tournamentId"]) },
        application = { context, operation -> if (operation == UiOperation.CREATE) context.requireApplication() else UiApplication.TOURNAMENT_MANAGER },
        relatedIds = { it.parentIdentifier?.let { id -> mapOf("tournamentId" to id) }.orEmpty() },
    )

    @GetMapping("/api/seasons/")
    @ResourceAccess(ResourceAction.LIST, "seasons")
    fun list(
        @RequestParam(defaultValue = "0") page: Int,
        @RequestParam(defaultValue = "20") size: Int,
        @RequestParam query: Map<String, String>,
        @RequestHeader(name = UI_APPLICATION_HEADER, required = false) application: String?,
        authentication: Authentication,
        response: HttpServletResponse,
    ): UiCollectionResponse<SeasonData> {

        return crud.list(ResourceRequest(authentication, application, query, page, size))
    }

    @GetMapping("/api/seasons/{identifier}/")
    @ResourceAccess(ResourceAction.READ, "seasons")
    fun detail(
        @PathVariable identifier: String,
        authentication: Authentication
    ): UiRecordResponse<SeasonData> =
        crud.find(identifier, ResourceRequest(authentication))

    @GetMapping("/api/seasons/create/")
    @ResourceAccess(ResourceAction.CREATE_FORM, "seasons")
    fun createForm(
        @ResourceLocator @RequestParam(required = false) tournamentId: String?,
        @RequestParam query: Map<String, String>,
        @RequestHeader(name = UI_APPLICATION_HEADER, required = false) application: String?,
        authentication: Authentication,
    ): UiRecordResponse<SeasonWriteRequest> = crud
        .createForm(ResourceRequest(authentication, application, query))

    @GetMapping("/api/seasons/{id}/edit/")
    @ResourceAccess(ResourceAction.UPDATE_FORM, "seasons")
    fun editForm(
        @ResourceLocator @PathVariable id: String,
        authentication: Authentication
    ): UiRecordResponse<SeasonWriteRequest> =
        crud.editForm(id, ResourceRequest(authentication))

    @PostMapping("/api/seasons/")
    @ResourceAccess(ResourceAction.CREATE, "seasons")
    fun create(
        @Valid @RequestBody body: SeasonWriteRequest,
        authentication: Authentication
    ): ResponseEntity<UiRecordResponse<SeasonData>> =
        crud.create(body, ResourceRequest(authentication))

    @PutMapping("/api/seasons/{id}/")
    @ResourceAccess(ResourceAction.UPDATE, "seasons")
    fun update(
        @ResourceLocator @PathVariable id: String, @Valid @RequestBody body: SeasonWriteRequest,
        authentication: Authentication
    ): UiRecordResponse<SeasonData> =
        crud.update(id, body, ResourceRequest(authentication))

    @GetMapping("/api/seasons/lookup/")
    @ResourceAccess(ResourceAction.LIST, "seasons")
    fun lookup(
        @RequestParam(defaultValue = "") q: String,
        @RequestParam(defaultValue = "20") limit: Int,
        @RequestParam(required = false) cursor: String?,
        @ResourceLocator @RequestParam(required = false) tournamentId: String?,
    ) = service.lookup(q, limit, cursor, tournamentId)

    @GetMapping("/api/seasons/naming-context/")
    @ResourceAccess(ResourceAction.CREATE_FORM, "seasons")
    fun namingContext(@ResourceLocator @RequestParam tournamentId: String) = service.namingContext(tournamentId)

    // The retired nested create URL must not be interpreted as a Season slug.
    @GetMapping("/api/tournaments/{tournamentIdentifier}/seasons/{identifier:(?!create$).+}/")
    @ResourceAccess(ResourceAction.READ, "seasons")
    fun detail(
        @PathVariable identifier: String, @PathVariable tournamentIdentifier: String,
        authentication: Authentication
    ) =
        crud.find(identifier, ResourceRequest(authentication, parentIdentifier = tournamentIdentifier))

    @GetMapping("/api/tournaments/{tournamentId}/seasons/")
    @ResourceAccess(ResourceAction.LIST, "seasons")
    fun list(
        @ResourceLocator @PathVariable tournamentId: String, @RequestParam(defaultValue = "0") page: Int,
        @RequestParam(defaultValue = "20") size: Int, authentication: Authentication
    ) =
        crud.list(ResourceRequest(authentication, page = page, size = size, parentIdentifier = tournamentId))

    @GetMapping("/api/tournaments/{tournamentId}/seasons/{seasonId}/edit/")
    @ResourceAccess(ResourceAction.UPDATE_FORM, "seasons")
    fun form(
        @ResourceLocator @PathVariable seasonId: String,
        @ResourceLocator @PathVariable tournamentId: String,
        authentication: Authentication
    ) =
        crud.editForm(seasonId, ResourceRequest(authentication, parentIdentifier = tournamentId))

    @PutMapping("/api/tournaments/{tournamentId}/seasons/{seasonId}/")
    @ResourceAccess(ResourceAction.UPDATE, "seasons")
    fun update(
        @ResourceLocator @PathVariable seasonId: String, @ResourceLocator @PathVariable tournamentId: String,
        @Valid @RequestBody request: SeasonWriteRequest, authentication: Authentication
    ) =
        crud.update(seasonId, request, ResourceRequest(authentication, parentIdentifier = tournamentId))
}
