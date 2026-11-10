package com.abeesmall.tournup.controller.season

import com.abeesmall.tournup.dto.audit.LastAuditValue
import com.abeesmall.tournup.dto.season.SeasonDetailResponse
import com.abeesmall.tournup.model.season.Season
import com.abeesmall.tournup.repository.audit.LatestAuditReader
import com.abeesmall.tournup.security.canManageSeasons
import com.abeesmall.tournup.security.canViewLastAudit
import com.abeesmall.tournup.service.season.SeasonDetailService
import java.util.UUID
import org.springframework.http.HttpStatus
import org.springframework.security.core.Authentication
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.server.ResponseStatusException

@RestController
class SeasonDetailController(
    private val service: SeasonDetailService,
    private val auditReader: LatestAuditReader,
) {
    @GetMapping(
        value = [
            "/api/seasons/{seasonId}/",
            "/api/tournaments/{tournamentId}/seasons/{seasonId}/",
        ],
    )
    fun detail(
        @PathVariable("seasonId") seasonId: UUID,
        @PathVariable(value = "tournamentId", required = false) tournamentId: UUID?,
        authentication: Authentication,
    ): SeasonDetailResponse {
        val response = service.find(
            id = seasonId,
            tournamentId = tournamentId,
            canUpdate = canManageSeasons(authentication),
        )
            ?: throw ResponseStatusException(HttpStatus.NOT_FOUND, "Season not found")

        if (!canViewLastAudit(authentication)) {
            return response
        }
        val latest = auditReader.findLatest(Season::class.java, listOf(response.item.id))

        return response.copy(
            item = response.item.copy(lastAudit = LastAuditValue(latest[response.item.id])),
        )
    }
}
