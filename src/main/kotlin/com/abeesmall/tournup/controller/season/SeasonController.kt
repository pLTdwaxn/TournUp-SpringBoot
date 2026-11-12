package com.abeesmall.tournup.controller.season

import com.abeesmall.tournup.dto.season.SeasonWriteRequest
import com.abeesmall.tournup.resource.ResourceAction
import com.abeesmall.tournup.resource.ResourceRequest
import com.abeesmall.tournup.resource.season.SeasonResourceHandler
import com.abeesmall.tournup.security.ResourceAccess
import com.abeesmall.tournup.service.season.SeasonService
import jakarta.validation.Valid
import java.util.UUID
import org.springframework.security.core.Authentication
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@RestController
class SeasonController(private val service: SeasonService, private val handler: SeasonResourceHandler) {
    @GetMapping("/api/seasons/lookup/")
    @ResourceAccess(ResourceAction.LIST, "seasons")
    fun lookup(
        @RequestParam(defaultValue = "") q: String,
        @RequestParam(defaultValue = "20") limit: Int,
        @RequestParam(required = false) cursor: String?,
        @RequestParam(required = false) tournamentId: UUID?,
    ) = service.lookup(q, limit, cursor, tournamentId)

    @GetMapping("/api/seasons/naming-context/")
    @ResourceAccess(ResourceAction.CREATE_FORM, "seasons")
    fun namingContext(@RequestParam tournamentId: UUID) = service.namingContext(tournamentId)

    // The retired nested create URL must not be interpreted as a Season slug.
    @GetMapping("/api/tournaments/{tournamentIdentifier}/seasons/{identifier:(?!create$).+}/")
    @ResourceAccess(ResourceAction.READ, "seasons")
    fun detail(@PathVariable identifier: String, @PathVariable tournamentIdentifier: String,
               authentication: Authentication) =
        handler.find(identifier, ResourceRequest(authentication, parentIdentifier = tournamentIdentifier))

    @GetMapping("/api/tournaments/{tournamentId}/seasons/")
    @ResourceAccess(ResourceAction.LIST, "seasons")
    fun list(@PathVariable tournamentId: UUID, @RequestParam(defaultValue = "0") page: Int,
             @RequestParam(defaultValue = "20") size: Int, authentication: Authentication) =
        handler.list(ResourceRequest(authentication, page = page, size = size, parentIdentifier = tournamentId.toString()))

    @GetMapping("/api/tournaments/{tournamentId}/seasons/{seasonId}/edit/")
    @ResourceAccess(ResourceAction.UPDATE_FORM, "seasons")
    fun form(@PathVariable seasonId: UUID, @PathVariable tournamentId: UUID, authentication: Authentication) =
        handler.editForm(seasonId, ResourceRequest(authentication, parentIdentifier = tournamentId.toString()))

    @PutMapping("/api/tournaments/{tournamentId}/seasons/{seasonId}/")
    @ResourceAccess(ResourceAction.UPDATE, "seasons")
    fun update(@PathVariable seasonId: UUID, @PathVariable tournamentId: UUID,
               @Valid @RequestBody request: SeasonWriteRequest, authentication: Authentication) =
        handler.updateTyped(seasonId, request, ResourceRequest(authentication, parentIdentifier = tournamentId.toString()))
}
