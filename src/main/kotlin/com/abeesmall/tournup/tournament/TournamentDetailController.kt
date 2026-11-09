package com.abeesmall.tournup.tournament

import com.abeesmall.tournup.security.canManageTournaments
import java.util.UUID
import org.springframework.http.HttpStatus
import org.springframework.security.core.Authentication
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.server.ResponseStatusException

@RestController
class TournamentDetailController(
    private val service: TournamentDetailService,
) {
    @GetMapping("/api/tournaments/{id}/")
    fun detail(
        @PathVariable("id") id: UUID,
        authentication: Authentication,
    ): TournamentDetailResponse =
        service.find(
            id = id,
            canUpdate = canManageTournaments(authentication),
        ) ?: throw ResponseStatusException(
            HttpStatus.NOT_FOUND,
            "Tournament not found",
        )
}
