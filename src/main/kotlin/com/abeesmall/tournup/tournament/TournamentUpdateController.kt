package com.abeesmall.tournup.tournament

import jakarta.validation.Valid
import java.util.UUID
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.server.ResponseStatusException

@RestController
class TournamentUpdateController(
    private val service: TournamentUpdateService,
) {
    @GetMapping("/api/tournaments/{id}/edit/")
    fun form(
        @PathVariable("id") id: UUID,
    ): TournamentUpdateFormResponse =
        service.form(id)
            ?: throw ResponseStatusException(
                HttpStatus.NOT_FOUND,
                "Tournament not found",
            )

    @PutMapping("/api/tournaments/{id}/")
    fun update(
        @PathVariable("id") id: UUID,
        @Valid @RequestBody request: TournamentUpdateRequest,
    ): TournamentDetailResponse =
        service.update(id, request)
            ?: throw ResponseStatusException(
                HttpStatus.NOT_FOUND,
                "Tournament not found",
            )
}
