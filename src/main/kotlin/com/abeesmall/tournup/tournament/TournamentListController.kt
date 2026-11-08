package com.abeesmall.tournup.tournament

import com.abeesmall.tournup.security.canManageTournaments

import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Sort
import org.springframework.http.HttpStatus
import org.springframework.security.core.Authentication
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.server.ResponseStatusException

@RestController
class TournamentListController(
    private val service: TournamentListService,
) {
    @GetMapping("/api/tournaments/")
    fun list(
        @RequestParam(defaultValue = "0") page: Int,
        @RequestParam(defaultValue = "20") size: Int,
        authentication: Authentication,
    ): TournamentListResponse {
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

        return service.list(
            PageRequest.of(page, size, Sort.by("name", "id")),
            canCreate = canManageTournaments(authentication),
        )
    }
}
