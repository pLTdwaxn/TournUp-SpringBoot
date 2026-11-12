package com.abeesmall.tournup.controller.resource

import com.abeesmall.tournup.resource.ResourceAction
import com.abeesmall.tournup.resource.ResourceRegistry
import com.abeesmall.tournup.resource.ResourceRequest
import com.abeesmall.tournup.security.ResourceAccess
import com.abeesmall.tournup.ui.UI_APPLICATION_HEADER
import jakarta.servlet.http.HttpServletResponse
import java.util.UUID
import org.springframework.security.core.Authentication
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestHeader
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import tools.jackson.databind.JsonNode

@RestController
class ResourceCrudController(private val registry: ResourceRegistry) {
    @GetMapping("/api/{resource}/")
    @ResourceAccess(ResourceAction.LIST)
    fun list(
        @PathVariable resource: String,
        @RequestParam(defaultValue = "0") page: Int,
        @RequestParam(defaultValue = "20") size: Int,
        @RequestParam query: Map<String, String>,
        @RequestHeader(name = UI_APPLICATION_HEADER, required = false) application: String?,
        authentication: Authentication,
        response: HttpServletResponse,
    ): Any {
        val handler = handler(resource)
        if (handler.listVariesByApplication) response.addHeader("Vary", UI_APPLICATION_HEADER)
        return handler.list(ResourceRequest(authentication, application, query, page, size))
    }

    @GetMapping("/api/{resource}/{identifier}/")
    @ResourceAccess(ResourceAction.READ)
    fun detail(@PathVariable resource: String, @PathVariable identifier: String,
               authentication: Authentication): Any =
        handler(resource).find(identifier, ResourceRequest(authentication))

    @GetMapping("/api/{resource}/create/")
    @ResourceAccess(ResourceAction.CREATE_FORM)
    fun createForm(
        @PathVariable resource: String,
        @RequestParam query: Map<String, String>,
        @RequestHeader(name = UI_APPLICATION_HEADER, required = false) application: String?,
        authentication: Authentication,
    ): Any = handler(resource)
        .createForm(ResourceRequest(authentication, application, query))

    @GetMapping("/api/{resource}/{id}/edit/")
    @ResourceAccess(ResourceAction.UPDATE_FORM)
    fun editForm(@PathVariable resource: String, @PathVariable id: UUID,
                 authentication: Authentication): Any =
        handler(resource).editForm(id, ResourceRequest(authentication))

    @PostMapping("/api/{resource}/")
    @ResourceAccess(ResourceAction.CREATE)
    fun create(@PathVariable resource: String, @RequestBody body: JsonNode,
               authentication: Authentication): Any =
        handler(resource).create(body, ResourceRequest(authentication))

    @PutMapping("/api/{resource}/{id}/")
    @ResourceAccess(ResourceAction.UPDATE)
    fun update(@PathVariable resource: String, @PathVariable id: UUID, @RequestBody body: JsonNode,
               authentication: Authentication): Any =
        handler(resource).update(id, body, ResourceRequest(authentication))

    private fun handler(resource: String) = registry.resources.getValue(resource)
}
