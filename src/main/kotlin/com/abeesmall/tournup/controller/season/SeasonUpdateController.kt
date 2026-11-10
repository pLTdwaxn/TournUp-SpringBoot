package com.abeesmall.tournup.controller.season

import com.abeesmall.tournup.dto.audit.LastAuditValue
import com.abeesmall.tournup.dto.season.SeasonDetailResponse
import com.abeesmall.tournup.dto.season.SeasonUpdateFormResponse
import com.abeesmall.tournup.dto.season.SeasonUpdateRequest
import com.abeesmall.tournup.model.season.Season
import com.abeesmall.tournup.repository.audit.LatestAuditReader
import com.abeesmall.tournup.security.canViewLastAudit
import com.abeesmall.tournup.service.season.SeasonUpdateService
import jakarta.validation.Valid
import java.util.UUID
import org.springframework.http.HttpStatus
import org.springframework.security.core.Authentication
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.server.ResponseStatusException

@RestController
class SeasonUpdateController(
    private val service: SeasonUpdateService,
    private val auditReader: LatestAuditReader,
) {
    @GetMapping(
        value = [
            "/api/seasons/{seasonId}/edit/",
            "/api/tournaments/{tournamentId}/seasons/{seasonId}/edit/",
        ],
    )
    fun form(
        @PathVariable("seasonId") seasonId: UUID,
        @PathVariable(value = "tournamentId", required = false) tournamentId: UUID?,
    ): SeasonUpdateFormResponse =
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
        @Valid @RequestBody request: SeasonUpdateRequest,
        authentication: Authentication,
    ): SeasonDetailResponse {
        val response = service.update(seasonId, request, tournamentId)
            ?: throw ResponseStatusException(HttpStatus.NOT_FOUND, "Season not found")

        if (!canViewLastAudit(authentication)) {
            return response
        }
        // The service transaction has committed the updated Envers revision.
        val audits = auditReader.findLatest(Season::class.java, listOf(response.item.id))
        return response.copy(
            item = response.item.copy(lastAudit = LastAuditValue(audits[response.item.id])),
        )
    }
}
