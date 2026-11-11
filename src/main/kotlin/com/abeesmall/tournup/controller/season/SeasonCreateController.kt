package com.abeesmall.tournup.controller.season

import com.abeesmall.tournup.dto.audit.LastAuditValue
import com.abeesmall.tournup.dto.season.SeasonCreateFormResponse
import com.abeesmall.tournup.dto.season.SeasonCreateRequest
import com.abeesmall.tournup.dto.season.SeasonDetailResponse
import com.abeesmall.tournup.model.season.Season
import com.abeesmall.tournup.repository.audit.LatestAuditReader
import com.abeesmall.tournup.security.canViewLastAudit
import com.abeesmall.tournup.service.season.SeasonCreateService
import jakarta.validation.Valid
import java.util.UUID
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.core.Authentication
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RestController

@RestController
class SeasonCreateController(
    private val service: SeasonCreateService,
    private val auditReader: LatestAuditReader,
) {
    @GetMapping("/api/tournaments/{tournamentId}/seasons/create/")
    fun form(
        @PathVariable("tournamentId") tournamentId: UUID,
    ): SeasonCreateFormResponse = service.form(tournamentId)

    @PostMapping("/api/tournaments/{tournamentId}/seasons/")
    fun create(
        @PathVariable("tournamentId") tournamentId: UUID,
        @Valid @RequestBody request: SeasonCreateRequest,
        authentication: Authentication,
    ): ResponseEntity<SeasonDetailResponse> {
        val created = service.create(tournamentId, request)
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
