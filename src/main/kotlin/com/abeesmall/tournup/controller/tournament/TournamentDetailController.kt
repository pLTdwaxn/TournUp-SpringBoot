package com.abeesmall.tournup.controller.tournament

import com.abeesmall.tournup.dto.audit.LastAuditValue
import com.abeesmall.tournup.dto.tournament.TournamentDetailResponse
import com.abeesmall.tournup.model.tournament.Tournament
import com.abeesmall.tournup.repository.audit.LatestAuditReader
import com.abeesmall.tournup.security.canManageTournaments
import com.abeesmall.tournup.security.canViewLastAudit
import com.abeesmall.tournup.service.tournament.TournamentDetailService

import org.springframework.http.HttpStatus
import org.springframework.security.core.Authentication
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.server.ResponseStatusException

@RestController
class TournamentDetailController(
    private val service: TournamentDetailService,
    private val auditReader: LatestAuditReader,
) {
    @GetMapping("/api/tournaments/{identifier}/")
    fun detail(
        @PathVariable("identifier") identifier: String,
        authentication: Authentication,
    ): TournamentDetailResponse {
        val response = service.find(
            identifier = identifier,
            canUpdate = canManageTournaments(authentication),
        ) ?: throw ResponseStatusException(
            HttpStatus.NOT_FOUND,
            "Tournament not found",
        )

        if (!canViewLastAudit(authentication)) {
            return response
        }

        val latest = auditReader.findLatest(
            Tournament::class.java,
            listOf(response.item.id),
        )[response.item.id]

        return response.copy(
            item = response.item.copy(
                lastAudit = LastAuditValue(latest),
            ),
        )
    }
}
