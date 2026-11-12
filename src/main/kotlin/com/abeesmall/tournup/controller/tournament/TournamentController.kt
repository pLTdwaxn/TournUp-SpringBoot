package com.abeesmall.tournup.controller.tournament

import com.abeesmall.tournup.dto.ui.LookupResponse
import com.abeesmall.tournup.resource.ResourceAction
import com.abeesmall.tournup.security.ResourceAccess
import com.abeesmall.tournup.service.tournament.TournamentService
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@RestController
class TournamentController(private val service: TournamentService) {
    @GetMapping("/api/tournaments/lookup/")
    @ResourceAccess(ResourceAction.LIST, "tournaments")
    fun lookup(
        @RequestParam(defaultValue = "") q: String,
        @RequestParam(defaultValue = "20") limit: Int,
        @RequestParam(required = false) cursor: String?,
    ): LookupResponse = service.lookup(q, limit, cursor)
}
