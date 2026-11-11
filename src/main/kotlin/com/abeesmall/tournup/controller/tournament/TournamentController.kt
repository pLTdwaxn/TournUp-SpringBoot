package com.abeesmall.tournup.controller.tournament

import com.abeesmall.tournup.dto.tournament.TournamentCreateRequest
import com.abeesmall.tournup.dto.tournament.TournamentData
import com.abeesmall.tournup.dto.tournament.TournamentUpdateRequest
import com.abeesmall.tournup.dto.ui.LookupResponse
import com.abeesmall.tournup.dto.ui.UiCollectionResponse
import com.abeesmall.tournup.dto.ui.UiRecordResponse
import com.abeesmall.tournup.manifest.tournament.tournamentResourceDefinition
import com.abeesmall.tournup.model.tournament.Tournament
import com.abeesmall.tournup.repository.audit.LatestAuditReader
import com.abeesmall.tournup.security.canManageTournaments
import com.abeesmall.tournup.service.resourcePage
import com.abeesmall.tournup.service.tournament.TournamentService
import com.abeesmall.tournup.service.withLatestAudit
import com.abeesmall.tournup.ui.UI_APPLICATION_HEADER
import com.abeesmall.tournup.ui.UiApplication
import com.abeesmall.tournup.ui.UiManifestContext
import com.abeesmall.tournup.ui.UiManifestResolver
import com.abeesmall.tournup.ui.UiOperation
import jakarta.servlet.http.HttpServletResponse
import jakarta.validation.Valid
import java.net.URI
import java.util.UUID
import org.springframework.data.domain.Sort
import org.springframework.http.HttpStatus
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
import org.springframework.web.server.ResponseStatusException


@RestController
class TournamentController(
    private val service: TournamentService,
    private val auditReader: LatestAuditReader,
) {

    @GetMapping("/api/tournaments/lookup/")
    fun lookup(
        @RequestParam(defaultValue = "") q: String,
        @RequestParam(defaultValue = "20") limit: Int,
        @RequestParam(required = false) cursor: String?,
    ): LookupResponse = service.lookup(q, limit, cursor)

    @GetMapping("/api/tournaments/create/")
    fun form(): UiRecordResponse<TournamentCreateRequest> =
        UiRecordResponse(UiManifestResolver.resolve(
            tournamentResourceDefinition(),
            UiManifestContext(
                UiApplication.TOURNAMENT_MANAGER,
                UiOperation.CREATE,
                allowedWrites = setOf(UiOperation.CREATE),
            ),
        ), item = TournamentCreateRequest(name = "", slug = ""))

    @PostMapping("/api/tournaments/")
    fun create(
        @Valid @RequestBody request: TournamentCreateRequest,
        authentication: Authentication,
    ): ResponseEntity<UiRecordResponse<TournamentData>> {
        val result = service.create(request)
        val location = URI.create("/api/tournaments/${result.item.id}/")

        return ResponseEntity.created(location).body(
            result.withLatestAudit(Tournament::class.java, authentication, auditReader),
        )
    }

    @GetMapping("/api/tournaments/{identifier}/")
    fun detail(
        @PathVariable("identifier") identifier: String,
        authentication: Authentication,
    ): UiRecordResponse<TournamentData> {
        val response = service.find(
            identifier = identifier,
            canUpdate = canManageTournaments(authentication),
        ) ?: throw ResponseStatusException(
            HttpStatus.NOT_FOUND,
            "Tournament not found",
        )

        return response.withLatestAudit(Tournament::class.java, authentication, auditReader)
    }

    @GetMapping("/api/tournaments/")
    fun list(
        @RequestParam(defaultValue = "0") page: Int,
        @RequestParam(defaultValue = "20") size: Int,
        @RequestHeader(name = UI_APPLICATION_HEADER) application: String,
        authentication: Authentication,
        servletResponse: HttpServletResponse,
    ): UiCollectionResponse<TournamentData> {
        servletResponse.addHeader("Vary", UI_APPLICATION_HEADER)

        val selectedApplication = UiApplication.fromHeader(application)
            ?: throw ResponseStatusException(
                HttpStatus.BAD_REQUEST,
                "Unknown $UI_APPLICATION_HEADER value",
            )
        val response = service.list(
            resourcePage(page, size, Sort.by("name", "id")),
            canCreate = canManageTournaments(authentication),
            application = selectedApplication,
        )

        return response.withLatestAudit(Tournament::class.java, authentication, auditReader)
    }

    @GetMapping("/api/tournaments/{id}/edit/")
    fun form(
        @PathVariable("id") id: UUID,
    ): UiRecordResponse<TournamentUpdateRequest> =
        service.form(id)
            ?: throw ResponseStatusException(
                HttpStatus.NOT_FOUND,
                "Tournament not found",
            )

    @PutMapping("/api/tournaments/{id}/")
    fun update(
        @PathVariable("id") id: UUID,
        @Valid @RequestBody request: TournamentUpdateRequest,
        authentication: Authentication,
    ): UiRecordResponse<TournamentData> {
        val result = service.update(id, request)
            ?: throw ResponseStatusException(
                HttpStatus.NOT_FOUND,
                "Tournament not found",
            )
        return result.withLatestAudit(Tournament::class.java, authentication, auditReader)
    }
}
