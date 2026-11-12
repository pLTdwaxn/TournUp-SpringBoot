package com.abeesmall.tournup.controller.resource

import com.abeesmall.tournup.dto.tournament.TournamentCreateRequest
import com.abeesmall.tournup.dto.tournament.TournamentData
import com.abeesmall.tournup.dto.tournament.TournamentUpdateRequest
import com.abeesmall.tournup.manifest.tournament.tournamentResourceDefinition
import com.abeesmall.tournup.model.tournament.Tournament
import com.abeesmall.tournup.repository.audit.LatestAuditReader
import com.abeesmall.tournup.resource.Resource
import com.abeesmall.tournup.resource.ResourceAction
import com.abeesmall.tournup.resource.ResourceDefinition
import com.abeesmall.tournup.resource.ResourceHandler
import com.abeesmall.tournup.resource.ResourceRegistry
import com.abeesmall.tournup.resource.ResourceRequest
import com.abeesmall.tournup.resource.tournament.tournamentCreateContract
import com.abeesmall.tournup.resource.tournament.tournamentUpdateContract
import com.abeesmall.tournup.security.SecurityConfiguration
import com.abeesmall.tournup.service.ResourceRecord
import com.abeesmall.tournup.service.tournament.TournamentService
import com.abeesmall.tournup.support.ResourceWebTestConfiguration
import com.abeesmall.tournup.ui.ResourceResponseComposer
import jakarta.validation.Validator
import java.util.UUID
import kotlin.test.assertFailsWith
import org.hamcrest.Matchers.equalTo
import org.junit.jupiter.api.Test
import org.mockito.Mockito.`when` as whenever
import org.mockito.Mockito.verifyNoInteractions
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Import
import org.springframework.http.MediaType
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user
import org.springframework.test.context.bean.override.mockito.MockitoBean
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.get
import org.springframework.test.web.servlet.post
import org.springframework.test.web.servlet.put
import tools.jackson.databind.json.JsonMapper

@WebMvcTest(ResourceCrudController::class)
@Import(ResourceWebTestConfiguration::class, SecurityConfiguration::class, ResourceRegistry::class, ResourceCrudControllerTests.Registration::class)
class ResourceCrudControllerTests(@Autowired private val mvc: MockMvc, @Autowired private val registry: ResourceRegistry) {
    @MockitoBean private lateinit var service: TournamentService
    @MockitoBean private lateinit var audits: LatestAuditReader

    @Resource(key = "sample", pluralName = "samples")
    class Sample

    @TestConfiguration(proxyBeanMethods = false)
    class Registration {
        @Bean
        fun sample(service: TournamentService, mapper: JsonMapper, validator: Validator, responses: ResourceResponseComposer) =
            object : ResourceHandler<TournamentCreateRequest, TournamentUpdateRequest, TournamentData>(
                tournamentResourceDefinition().copy(definition = ResourceDefinition(Sample::class.java, tournamentCreateContract, tournamentUpdateContract,
                    actions = setOf(ResourceAction.CREATE, ResourceAction.READ),
                    canRead = { it.authorities.any { role -> role.authority == "ROLE_RESOURCE_EDITOR" } },
                    canWrite = { it.authorities.any { role -> role.authority == "ROLE_RESOURCE_EDITOR" } })),
                tournamentCreateContract, tournamentUpdateContract, mapper, validator, responses, Tournament::class.java,
            ) {
                override fun editFormData(id: UUID, context: ResourceRequest) = error("Unsupported")
                override fun title(item: TournamentData) = item.name
                override fun listRecords(context: ResourceRequest) = error("Unsupported")
                override fun findRecord(identifier: String, context: ResourceRequest) = service.find(identifier)
                override fun createRecord(request: TournamentCreateRequest, context: ResourceRequest) = service.create(request)
                override fun updateRecord(id: UUID, request: TournamentUpdateRequest, context: ResourceRequest) = error("Unsupported")
            }
    }

    @Test
    fun `registry authorizes before attempting to parse a request body`() {
        mvc.post("/api/samples/") {
            with(user("reader").roles("PLAYER")); with(csrf())
            contentType = MediaType.APPLICATION_JSON
            content = "not-json"
        }.andExpect { status { isForbidden() } }
        verifyNoInteractions(service, audits)
    }

    @Test
    fun `registry enforces permissions for routes outside the security URL list`() {
        mvc.post("/api/samples/") {
            with(user("reader").roles("PLAYER")); with(csrf())
            contentType = MediaType.APPLICATION_JSON
            content = """{"name":"Sample","slug":"sample"}"""
        }.andExpect { status { isForbidden() } }
        mvc.get("/api/samples/sample/") { with(user("reader").roles("PLAYER")) }
            .andExpect { status { isForbidden() } }
        verifyNoInteractions(service, audits)
    }

    @Test
    fun `unknown resources and disabled operations do not dispatch`() {
        mvc.get("/api/unregistered/") { with(user("reader").roles("PLAYER")) }
            .andExpect { status { isNotFound() } }
        mvc.put("/api/samples/${UUID.randomUUID()}/") {
            with(user("writer").roles("RESOURCE_EDITOR")); with(csrf())
            contentType = MediaType.APPLICATION_JSON; content = "{}"
        }.andExpect { status { isMethodNotAllowed() } }
        verifyNoInteractions(service, audits)
    }

    @Test
    fun `registered handler binds its DTO and applies create defaults`() {
        val request = TournamentCreateRequest("Sample", "sample")
        val item = TournamentData(UUID.randomUUID(), "sample", "Sample", null,
            request.participantType, request.participationMode, request.handicapMode)
        whenever(service.create(request)).thenReturn(ResourceRecord(item))
        mvc.post("/api/samples/") {
            with(user("writer").roles("RESOURCE_EDITOR")); with(csrf())
            contentType = MediaType.APPLICATION_JSON
            content = """{"name":"Sample","slug":"sample"}"""
        }.andExpect {
            status { isCreated() }
            jsonPath("$.item.participantType", equalTo("PLAYER"))
            jsonPath("$.manifest.commands") { isEmpty() }
        }
    }

    @Test
    fun `duplicate handler registrations fail at startup`() {
        val handler = registry.resources.getValue("samples")
        assertFailsWith<IllegalArgumentException> { ResourceRegistry(listOf(handler, handler)) }
    }
}
