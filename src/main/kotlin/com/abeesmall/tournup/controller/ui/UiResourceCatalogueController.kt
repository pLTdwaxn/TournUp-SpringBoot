package com.abeesmall.tournup.controller.ui

import com.abeesmall.tournup.auth.AuthenticatedUser
import com.abeesmall.tournup.dto.ui.UiResourceCatalogue
import com.abeesmall.tournup.service.ui.UiResourceCatalogueService
import com.abeesmall.tournup.ui.UI_APPLICATION_HEADER
import com.abeesmall.tournup.ui.UiApplication
import java.nio.charset.StandardCharsets.UTF_8
import java.security.MessageDigest
import java.util.HexFormat
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.core.Authentication
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestHeader
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.context.request.WebRequest
import org.springframework.web.server.ResponseStatusException
import tools.jackson.databind.json.JsonMapper

@RestController
class UiResourceCatalogueController(
    private val service: UiResourceCatalogueService,
    private val mapper: JsonMapper,
) {
    @GetMapping("/api/ui/resources/")
    fun catalogue(
        @RequestHeader(name = UI_APPLICATION_HEADER) application: String,
        authentication: Authentication,
        webRequest: WebRequest,
    ): ResponseEntity<UiResourceCatalogue> {
        val selectedApplication = UiApplication.fromHeader(application)
            ?: throw ResponseStatusException(HttpStatus.BAD_REQUEST, "Unknown $UI_APPLICATION_HEADER value")
        val user = authentication.principal as? AuthenticatedUser
            ?: throw ResponseStatusException(HttpStatus.FORBIDDEN, "An authenticated tenant user is required")
        val catalogue = service.catalogue(selectedApplication, authentication, user)
        val digest = MessageDigest.getInstance("SHA-256")
        digest.update(mapper.writeValueAsBytes(catalogue))
        digest.update(authentication.authorities.mapNotNull { it.authority }.sorted().joinToString("\n").toByteArray(UTF_8))
        val etag = "W/\"${HexFormat.of().formatHex(digest.digest())}\""

        val headers = HttpHeaders().apply {
            set(HttpHeaders.CACHE_CONTROL, "private, no-cache")
            set(HttpHeaders.VARY, "$UI_APPLICATION_HEADER, Cookie, Authorization")
            set(HttpHeaders.ETAG, etag)
        }
        return if (webRequest.checkNotModified(etag)) {
            ResponseEntity.status(HttpStatus.NOT_MODIFIED).headers(headers).build()
        } else {
            ResponseEntity.ok().headers(headers).body(catalogue)
        }
    }
}
