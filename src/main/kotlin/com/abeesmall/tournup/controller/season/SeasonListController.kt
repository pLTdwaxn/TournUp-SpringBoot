package com.abeesmall.tournup.controller.season

import com.abeesmall.tournup.dto.audit.LastAuditValue
import com.abeesmall.tournup.dto.season.SeasonListResponse
import com.abeesmall.tournup.model.season.Season
import com.abeesmall.tournup.repository.audit.LatestAuditReader
import com.abeesmall.tournup.security.canManageSeasons
import com.abeesmall.tournup.security.canViewLastAudit
import com.abeesmall.tournup.service.season.SeasonListService
import java.util.UUID
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Sort
import org.springframework.http.HttpStatus
import org.springframework.security.core.Authentication
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.server.ResponseStatusException

@RestController
class SeasonListController(
    private val service: SeasonListService,
    private val auditReader: LatestAuditReader,
) {
    @GetMapping(value = ["/api/seasons/", "/api/tournaments/{tournamentId}/seasons/"])
    fun list(
        @PathVariable(value = "tournamentId", required = false) tournamentId: UUID?,
        @RequestParam(defaultValue = "0") page: Int,
        @RequestParam(defaultValue = "20") size: Int,
        authentication: Authentication,
    ): SeasonListResponse {
        if (page < 0) {
            throw ResponseStatusException(HttpStatus.BAD_REQUEST, "page must be zero or greater")
        }
        if (size !in 1..100) {
            throw ResponseStatusException(HttpStatus.BAD_REQUEST, "size must be between 1 and 100")
        }

        val response = service.list(
            tournamentId = tournamentId,
            pageable = PageRequest.of(
                page,
                size,
                Sort.by(Sort.Order.desc("fromYear"), Sort.Order.asc("name"), Sort.Order.asc("id")),
            ),
            canCreate = canManageSeasons(authentication),
        )
        if (!canViewLastAudit(authentication) || response.items.isEmpty()) {
            return response
        }

        val audits = auditReader.findLatest(Season::class.java, response.items.map { it.id })
        return response.copy(
            items = response.items.map { item ->
                item.copy(lastAudit = LastAuditValue(audits[item.id]))
            },
        )
    }
}
