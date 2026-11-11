package com.abeesmall.tournup.controller.season

import com.abeesmall.tournup.dto.audit.LastAuditValue
import com.abeesmall.tournup.dto.season.SeasonCreateFormResponse
import com.abeesmall.tournup.dto.season.SeasonCreateRequest
import com.abeesmall.tournup.dto.season.SeasonDetailResponse
import com.abeesmall.tournup.dto.season.SeasonNamingContext
import com.abeesmall.tournup.model.season.Season
import com.abeesmall.tournup.repository.audit.LatestAuditReader
import com.abeesmall.tournup.security.canViewLastAudit
import com.abeesmall.tournup.service.season.SeasonCreateService
import com.abeesmall.tournup.ui.UI_APPLICATION_HEADER
import com.abeesmall.tournup.ui.UiApplication
import jakarta.validation.Valid
import java.util.UUID
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.core.Authentication
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RequestHeader
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.server.ResponseStatusException

@RestController
class SeasonCreateController(
    private val service: SeasonCreateService,
    private val auditReader: LatestAuditReader,
) {
    @GetMapping("/api/seasons/create/")
    fun form(
        @RequestHeader(name = UI_APPLICATION_HEADER) application: String,
        @RequestParam(required = false) tournamentId: UUID?,
    ): SeasonCreateFormResponse {
        val selected = UiApplication.fromHeader(application)
            ?: throw ResponseStatusException(HttpStatus.BAD_REQUEST, "Unknown $UI_APPLICATION_HEADER value")
        return service.form(selected, tournamentId)
    }

    @GetMapping("/api/seasons/naming-context/")
    fun namingContext(@RequestParam tournamentId: UUID): SeasonNamingContext = service.namingContext(tournamentId)

    @PostMapping("/api/seasons/")
    fun create(
        @Valid @RequestBody request: SeasonCreateRequest,
        authentication: Authentication,
    ): ResponseEntity<SeasonDetailResponse> {
        val created = service.create(request)
        // Envers writes the revision when the service transaction commits.
        val item = created.item
        val response = if (canViewLastAudit(authentication)) {
            created.copy(
                item = item.copy(
                    lastAudit = LastAuditValue(
                        auditReader.findLatest(Season::class.java, listOf(item.id))[item.id],
                    ),
                ),
            )
        } else {
            created
        }

        return ResponseEntity.status(HttpStatus.CREATED).body(response)
    }
}
