package com.abeesmall.tournup.controller.tournament

import com.abeesmall.tournup.dto.tournament.TournamentCreateFormResponse
import com.abeesmall.tournup.dto.tournament.TournamentCreateRequest
import com.abeesmall.tournup.dto.tournament.TournamentDetailResponse
import com.abeesmall.tournup.manifest.tournament.tournamentCreateManifest
import com.abeesmall.tournup.service.tournament.TournamentCreateService
import jakarta.validation.Valid
import java.net.URI
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RestController

@RestController
class TournamentCreateController(
    private val service: TournamentCreateService,
) {
    @GetMapping("/api/tournaments/create/")
    fun form(): TournamentCreateFormResponse =
        TournamentCreateFormResponse(tournamentCreateManifest())

    @PostMapping("/api/tournaments/")
    fun create(
        @Valid @RequestBody request: TournamentCreateRequest,
    ): ResponseEntity<TournamentDetailResponse> {
        val result = service.create(request)
        val location = URI.create("/api/tournaments/${result.item.id}/")

        return ResponseEntity.created(location).body(result)
    }
}
