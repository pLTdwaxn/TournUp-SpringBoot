package com.abeesmall.tournup.controller.tournament

import com.abeesmall.tournup.dto.tournament.TournamentCreateRequest
import com.abeesmall.tournup.dto.tournament.TournamentData
import com.abeesmall.tournup.dto.tournament.TournamentUpdateRequest
import com.abeesmall.tournup.dto.ui.LookupResponse
import com.abeesmall.tournup.dto.ui.UiCollectionResponse
import com.abeesmall.tournup.dto.ui.UiRecordResponse
import com.abeesmall.tournup.dto.tournament.tournamentCreateContract
import com.abeesmall.tournup.manifest.tournament.tournamentResourceDefinition
import com.abeesmall.tournup.model.tournament.Tournament
import com.abeesmall.tournup.security.ResourceAccess
import com.abeesmall.tournup.service.CrudDelegate
import com.abeesmall.tournup.service.ResourceRequest
import com.abeesmall.tournup.service.resourcePage
import com.abeesmall.tournup.service.tournament.TournamentService
import com.abeesmall.tournup.ui.ResourceResponseComposer
import com.abeesmall.tournup.ui.UI_APPLICATION_HEADER
import com.abeesmall.tournup.ui.UiApplication
import com.abeesmall.tournup.ui.UiOperation
import com.abeesmall.tournup.ui.resource.ResourceAction
import jakarta.servlet.http.HttpServletResponse
import com.abeesmall.tournup.validation.ResourceLocator
import jakarta.validation.Valid
import java.net.URI
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
class TournamentController(private val service: TournamentService, responses: ResourceResponseComposer) {
    private val crud = CrudDelegate<TournamentCreateRequest, TournamentUpdateRequest, TournamentData>(
        tournamentResourceDefinition(), responses, Tournament::class.java,
        title = { it.name },
        createFormData = { com.abeesmall.tournup.service.ResourceRecord(requireNotNull(tournamentCreateContract.initialValues).invoke()) },
        listRecords = { service.list(resourcePage(it.page, it.size, Sort.by("name", "id"))) },
        findRecord = { id, _ -> service.find(id) },
        createRecord = { input, _ -> service.create(input) },
        updateRecord = { id, input, _ -> service.update(id, input) },
        editFormData = { id, _ -> service.editForm(id) },
        application = { context, operation -> if (operation == UiOperation.LIST) context.requireApplication() else UiApplication.TOURNAMENT_MANAGER },
        creationLocation = { URI.create("/api/tournaments/${it.item.id}/") },
    )

    @GetMapping("/api/tournaments/")
    @ResourceAccess(ResourceAction.LIST, "tournaments")
    fun list(
        @RequestParam(defaultValue = "0") page: Int,
        @RequestParam(defaultValue = "20") size: Int,
        @RequestParam query: Map<String, String>,
        @RequestHeader(name = UI_APPLICATION_HEADER, required = false) application: String?,
        authentication: Authentication,
        response: HttpServletResponse,
    ): UiCollectionResponse<TournamentData> {
        response.addHeader("Vary", UI_APPLICATION_HEADER)
        return crud.list(ResourceRequest(authentication, application, query, page, size))
    }

    @GetMapping("/api/tournaments/{identifier}/")
    @ResourceAccess(ResourceAction.READ, "tournaments")
    fun detail(@PathVariable identifier: String,
               authentication: Authentication): UiRecordResponse<TournamentData> =
        crud.find(identifier, ResourceRequest(authentication))

    @GetMapping("/api/tournaments/create/")
    @ResourceAccess(ResourceAction.CREATE_FORM, "tournaments")
    fun createForm(
        @RequestParam query: Map<String, String>,
        @RequestHeader(name = UI_APPLICATION_HEADER, required = false) application: String?,
        authentication: Authentication,
    ): UiRecordResponse<TournamentCreateRequest> = crud
        .createForm(ResourceRequest(authentication, application, query))

    @GetMapping("/api/tournaments/{id}/edit/")
    @ResourceAccess(ResourceAction.UPDATE_FORM, "tournaments")
    fun editForm(@ResourceLocator @PathVariable id: String,
                 authentication: Authentication): UiRecordResponse<TournamentUpdateRequest> =
        crud.editForm(id, ResourceRequest(authentication))

    @PostMapping("/api/tournaments/")
    @ResourceAccess(ResourceAction.CREATE, "tournaments")
    fun create(@Valid @RequestBody body: TournamentCreateRequest,
               authentication: Authentication): ResponseEntity<UiRecordResponse<TournamentData>> =
        crud.create(body, ResourceRequest(authentication))

    @PutMapping("/api/tournaments/{id}/")
    @ResourceAccess(ResourceAction.UPDATE, "tournaments")
    fun update(@ResourceLocator @PathVariable id: String, @Valid @RequestBody body: TournamentUpdateRequest,
               authentication: Authentication): UiRecordResponse<TournamentData> =
        crud.update(id, body, ResourceRequest(authentication))

    @GetMapping("/api/tournaments/lookup/")
    @ResourceAccess(ResourceAction.LIST, "tournaments")
    fun lookup(
        @RequestParam(defaultValue = "") q: String,
        @RequestParam(defaultValue = "20") limit: Int,
        @RequestParam(required = false) cursor: String?,
    ): LookupResponse = service.lookup(q, limit, cursor)
}
