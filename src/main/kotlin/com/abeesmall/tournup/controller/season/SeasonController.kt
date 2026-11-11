package com.abeesmall.tournup.controller.season

import com.abeesmall.tournup.dto.season.SeasonData
import com.abeesmall.tournup.dto.season.SeasonNamingContext
import com.abeesmall.tournup.dto.season.SeasonWriteRequest
import com.abeesmall.tournup.dto.ui.LookupResponse
import com.abeesmall.tournup.dto.ui.UiCollectionResponse
import com.abeesmall.tournup.dto.ui.UiRecordResponse
import com.abeesmall.tournup.model.season.Season
import com.abeesmall.tournup.repository.audit.LatestAuditReader
import com.abeesmall.tournup.security.canManageSeasons
import com.abeesmall.tournup.service.resourcePage
import com.abeesmall.tournup.service.season.SeasonService
import com.abeesmall.tournup.service.withLatestAudit
import com.abeesmall.tournup.ui.UI_APPLICATION_HEADER
import com.abeesmall.tournup.ui.UiApplication
import jakarta.validation.Valid
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
class SeasonController(
    private val service: SeasonService,
    private val auditReader: LatestAuditReader,
) {

    @GetMapping("/api/seasons/lookup/")
    fun lookup(
        @RequestParam(defaultValue = "") q: String,
        @RequestParam(defaultValue = "20") limit: Int,
        @RequestParam(required = false) cursor: String?,
        @RequestParam(required = false) tournamentId: UUID?,
    ): LookupResponse = service.lookup(q, limit, cursor, tournamentId)
    @GetMapping("/api/seasons/create/")
    fun form(
        @RequestHeader(name = UI_APPLICATION_HEADER) application: String,
        @RequestParam(required = false) tournamentId: UUID?,
    ): UiRecordResponse<SeasonWriteRequest> {
        val selected = UiApplication.fromHeader(application)
            ?: throw ResponseStatusException(HttpStatus.BAD_REQUEST, "Unknown $UI_APPLICATION_HEADER value")
        return service.form(selected, tournamentId)
    }

    @GetMapping("/api/seasons/naming-context/")
    fun namingContext(@RequestParam tournamentId: UUID): SeasonNamingContext = service.namingContext(tournamentId)

    @PostMapping("/api/seasons/")
    fun create(
        @Valid @RequestBody request: SeasonWriteRequest,
        authentication: Authentication,
    ): ResponseEntity<UiRecordResponse<SeasonData>> {
        val created = service.create(request)
        return ResponseEntity.status(HttpStatus.CREATED).body(
            created.withLatestAudit(Season::class.java, authentication, auditReader),
        )
    }

    @GetMapping(
        value = [
            "/api/seasons/{identifier}/",
            // The retired nested create URL must not be interpreted as a Season slug.
            "/api/tournaments/{tournamentIdentifier}/seasons/{identifier:(?!create$).+}/",
        ],
    )
    fun detail(
        @PathVariable("identifier") identifier: String,
        @PathVariable(value = "tournamentIdentifier", required = false) tournamentIdentifier: String?,
        authentication: Authentication,
    ): UiRecordResponse<SeasonData> {
        val response = service.find(
            identifier = identifier,
            tournamentIdentifier = tournamentIdentifier,
            canUpdate = canManageSeasons(authentication),
        )
            ?: throw ResponseStatusException(HttpStatus.NOT_FOUND, "Season not found")

        return response.withLatestAudit(Season::class.java, authentication, auditReader)
    }

    @GetMapping(value = ["/api/seasons/", "/api/tournaments/{tournamentId}/seasons/"])
    fun list(
        @PathVariable(value = "tournamentId", required = false) tournamentId: UUID?,
        @RequestParam(defaultValue = "0") page: Int,
        @RequestParam(defaultValue = "20") size: Int,
        authentication: Authentication,
    ): UiCollectionResponse<SeasonData> {

        val response = service.list(
            tournamentId = tournamentId,
            pageable = resourcePage(
                page,
                size,
                Sort.by(Sort.Order.desc("fromYear"), Sort.Order.asc("name"), Sort.Order.asc("id")),
            ),
            canCreate = canManageSeasons(authentication),
        )
        return response.withLatestAudit(Season::class.java, authentication, auditReader)
    }

    @GetMapping(
        value = [
            "/api/seasons/{seasonId}/edit/",
            "/api/tournaments/{tournamentId}/seasons/{seasonId}/edit/",
        ],
    )
    fun form(
        @PathVariable("seasonId") seasonId: UUID,
        @PathVariable(value = "tournamentId", required = false) tournamentId: UUID?,
    ): UiRecordResponse<SeasonWriteRequest> =
        service.form(seasonId, tournamentId)
            ?: throw ResponseStatusException(HttpStatus.NOT_FOUND, "Season not found")

    @PutMapping(
        value = [
            "/api/seasons/{seasonId}/",
            "/api/tournaments/{tournamentId}/seasons/{seasonId}/",
        ],
    )
    fun update(
        @PathVariable("seasonId") seasonId: UUID,
        @PathVariable(value = "tournamentId", required = false) tournamentId: UUID?,
        @Valid @RequestBody request: SeasonWriteRequest,
        authentication: Authentication,
    ): UiRecordResponse<SeasonData> {
        val response = service.update(seasonId, request, tournamentId)
            ?: throw ResponseStatusException(HttpStatus.NOT_FOUND, "Season not found")

        return response.withLatestAudit(Season::class.java, authentication, auditReader)
    }
}
