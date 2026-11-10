package com.abeesmall.tournup.controller.tournament

import com.abeesmall.tournup.dto.audit.LastAuditValue
import com.abeesmall.tournup.dto.tournament.TournamentListResponse
import com.abeesmall.tournup.model.tournament.Tournament
import com.abeesmall.tournup.repository.audit.LatestAuditReader
import com.abeesmall.tournup.security.canManageTournaments
import com.abeesmall.tournup.security.canViewLastAudit

import com.abeesmall.tournup.service.tournament.TournamentListService
import com.abeesmall.tournup.ui.UI_APPLICATION_HEADER
import com.abeesmall.tournup.ui.UiApplication
import jakarta.servlet.http.HttpServletResponse
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Sort
import org.springframework.http.HttpStatus
import org.springframework.security.core.Authentication
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestHeader
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.server.ResponseStatusException

@RestController
class TournamentListController(
    private val service: TournamentListService,
    private val auditReader: LatestAuditReader,
) {
    @GetMapping("/api/tournaments/")
    fun list(
        @RequestParam(defaultValue = "0") page: Int,
        @RequestParam(defaultValue = "20") size: Int,
        @RequestHeader(name = UI_APPLICATION_HEADER) application: String,
        authentication: Authentication,
        servletResponse: HttpServletResponse,
    ): TournamentListResponse {
        servletResponse.addHeader("Vary", UI_APPLICATION_HEADER)
        if (page < 0) {
            throw ResponseStatusException(
                HttpStatus.BAD_REQUEST,
                "page must be zero or greater",
            )
        }
        if (size !in 1..100) {
            throw ResponseStatusException(
                HttpStatus.BAD_REQUEST,
                "size must be between 1 and 100",
            )
        }

        val selectedApplication = UiApplication.fromHeader(application)
            ?: throw ResponseStatusException(
                HttpStatus.BAD_REQUEST,
                "Unknown $UI_APPLICATION_HEADER value",
            )
        val response = service.list(
            PageRequest.of(page, size, Sort.by("name", "id")),
            canCreate = canManageTournaments(authentication),
            application = selectedApplication,
        )

        if (!canViewLastAudit(authentication)) {
            return response
        }

        val lastAudits = auditReader.findLatest(
            Tournament::class.java,
            response.items.map { it.id },
        )

        return response.copy(
            items = response.items.map { item ->
                item.copy(
                    lastAudit = LastAuditValue(lastAudits[item.id]),
                )
            },
        )
    }
}
