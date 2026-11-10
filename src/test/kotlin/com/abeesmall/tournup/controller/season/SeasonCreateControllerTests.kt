package com.abeesmall.tournup.controller.season

import com.abeesmall.tournup.dto.audit.LastAuditData
import com.abeesmall.tournup.dto.season.SeasonCreateContext
import com.abeesmall.tournup.dto.season.SeasonCreateFormResponse
import com.abeesmall.tournup.dto.season.SeasonCreateRequest
import com.abeesmall.tournup.dto.season.SeasonData
import com.abeesmall.tournup.manifest.season.seasonCreateManifest
import com.abeesmall.tournup.model.season.Season
import com.abeesmall.tournup.repository.audit.LatestAuditReader
import com.abeesmall.tournup.security.SecurityConfiguration
import com.abeesmall.tournup.service.season.SeasonCreateService
import com.abeesmall.tournup.service.season.SeasonFieldConflictException
import com.abeesmall.tournup.service.season.SeasonTournamentNotFoundException
import java.time.Instant
import java.util.UUID
import org.hamcrest.Matchers.contains
import org.hamcrest.Matchers.equalTo
import org.hamcrest.Matchers.greaterThan
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import org.mockito.Mockito.`when` as whenever
import org.mockito.Mockito.inOrder
import org.mockito.Mockito.verifyNoInteractions
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest
import org.springframework.context.annotation.Import
import org.springframework.http.MediaType
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user
import org.springframework.test.context.bean.override.mockito.MockitoBean
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.get
import org.springframework.test.web.servlet.post
import tools.jackson.databind.json.JsonMapper

@WebMvcTest(SeasonCreateController::class)
@Import(SecurityConfiguration::class)
class SeasonCreateControllerTests(
    @Autowired private val mockMvc: MockMvc,
    @Autowired private val mapper: JsonMapper,
) {
    @MockitoBean
    private lateinit var service: SeasonCreateService

    @MockitoBean
    private lateinit var auditReader: LatestAuditReader

    private val tournamentId = UUID.randomUUID()
    private val formUrl = "/api/tournaments/$tournamentId/seasons/create/"
    private val createUrl = "/api/tournaments/$tournamentId/seasons/"
    private val request = SeasonCreateRequest("2026 S4", "2026-s4", 2026)
    private val created = SeasonData(UUID.randomUUID(), tournamentId, "2026 S4", "2026-s4", 2026)

    @ParameterizedTest
    @ValueSource(strings = ["SUPER_ADMIN", "CLUB_MANAGER", "TOURNAMENT_MANAGER"])
    fun `writers receive the parent scoped form and naming context`(role: String) {
        whenever(service.form(tournamentId)).thenReturn(
            SeasonCreateFormResponse(
                manifest = seasonCreateManifest(tournamentId),
                context = SeasonCreateContext(
                    tournamentId,
                    mapOf(2026 to listOf("2026 S1", "2026 S3")),
                ),
            ),
        )

        mockMvc.get(formUrl) {
            with(user("writer").roles(role))
        }.andExpect {
            status { isOk() }
            jsonPath("$.manifest.operation", equalTo("create"))
            jsonPath("$.manifest.fields.fromYear.dataType", equalTo("integer"))
            jsonPath("$.manifest.fields.fromYear.widget", equalTo("number"))
            jsonPath("$.manifest.fields.name.behaviours[0].type", equalTo("season-name-from-year"))
            jsonPath("$.manifest.fields.slug.behaviours[0].type", equalTo("slugify"))
            jsonPath("$.manifest.commands[0].href", equalTo(createUrl))
            jsonPath("$.context.tournamentId", equalTo(tournamentId.toString()))
            jsonPath("$.context.existingNamesByYear['2026']", contains("2026 S1", "2026 S3"))
            jsonPath("$.item.name", equalTo(""))
            jsonPath("$.item.slug", equalTo(""))
            jsonPath("$.item.fromYear") { hasJsonPath(); value(null) }
        }
        verifyNoInteractions(auditReader)
    }

    @ParameterizedTest
    @ValueSource(strings = ["SUPER_ADMIN", "CLUB_MANAGER", "TOURNAMENT_MANAGER"])
    fun `writers create a season and receive its read manifest and committed audit`(role: String) {
        val actorId = UUID.randomUUID()
        whenever(service.create(tournamentId, request)).thenReturn(created)
        whenever(auditReader.findLatest(Season::class.java, listOf(created.id))).thenReturn(
            mapOf(created.id to LastAuditData(42, Instant.parse("2026-11-10T00:00:00Z"), actorId)),
        )

        mockMvc.post(createUrl) {
            with(user("writer").roles(role))
            with(csrf())
            contentType = MediaType.APPLICATION_JSON
            content = mapper.writeValueAsString(request)
        }.andExpect {
            status { isCreated() }
            content { contentType("application/json") }
            jsonPath("$.manifest.resource", equalTo("season"))
            jsonPath("$.manifest.operation", equalTo("read"))
            jsonPath("$.manifest.layout.type", equalTo("detail"))
            jsonPath("$.manifest.layout.sections[0].fields", contains("fromYear", "name", "slug"))
            jsonPath("$.manifest.fields.fromYear.dataType", equalTo("integer"))
            jsonPath("$.manifest.fields.fromYear.widget", equalTo("display"))
            jsonPath("$.manifest.fields.name.editable", equalTo(false))
            jsonPath("$.manifest.fields.slug.editable", equalTo(false))
            jsonPath("$.manifest.commands.length()", equalTo(0))
            jsonPath("$.item.id", equalTo(created.id.toString()))
            jsonPath("$.item.tournamentId", equalTo(tournamentId.toString()))
            jsonPath("$.item.name", equalTo(request.name))
            jsonPath("$.item.slug", equalTo(request.slug))
            jsonPath("$.item.fromYear", equalTo(2026))
            jsonPath("$.item.lastAudit.revision", equalTo(42))
            jsonPath("$.item.lastAudit.timestamp", equalTo("2026-11-10T00:00:00Z"))
            jsonPath("$.item.lastAudit.actorId", equalTo(actorId.toString()))
        }

        val ordered = inOrder(service, auditReader)
        ordered.verify(service).create(tournamentId, request)
        ordered.verify(auditReader).findLatest(Season::class.java, listOf(created.id))
        ordered.verifyNoMoreInteractions()
    }

    @Test
    fun `missing audit history is explicitly null`() {
        whenever(service.create(tournamentId, request)).thenReturn(created)
        whenever(auditReader.findLatest(Season::class.java, listOf(created.id))).thenReturn(emptyMap())

        mockMvc.post(createUrl) {
            with(user("writer").roles("SUPER_ADMIN"))
            with(csrf())
            contentType = MediaType.APPLICATION_JSON
            content = mapper.writeValueAsString(request)
        }.andExpect {
            status { isCreated() }
            jsonPath("$.item.lastAudit") { hasJsonPath(); value(null) }
        }
    }

    @ParameterizedTest
    @ValueSource(strings = ["PLAYER", "COACH"])
    fun `read only roles cannot open the form or create a season`(role: String) {
        mockMvc.get(formUrl) {
            with(user("reader").roles(role))
        }.andExpect { status { isForbidden() } }
        mockMvc.post(createUrl) {
            with(user("reader").roles(role))
            with(csrf())
            contentType = MediaType.APPLICATION_JSON
            content = mapper.writeValueAsString(request)
        }.andExpect { status { isForbidden() } }

        verifyNoInteractions(service, auditReader)
    }

    @Test
    fun `both endpoints require authentication`() {
        mockMvc.get(formUrl).andExpect { status { isUnauthorized() } }
        mockMvc.post(createUrl) {
            with(csrf())
            contentType = MediaType.APPLICATION_JSON
            content = mapper.writeValueAsString(request)
        }.andExpect { status { isUnauthorized() } }

        verifyNoInteractions(service, auditReader)
    }

    @Test
    fun `creation requires csrf`() {
        mockMvc.post(createUrl) {
            with(user("writer").roles("SUPER_ADMIN"))
            contentType = MediaType.APPLICATION_JSON
            content = mapper.writeValueAsString(request)
        }.andExpect { status { isForbidden() } }

        verifyNoInteractions(service, auditReader)
    }

    @Test
    fun `invalid fields return field errors before calling the service`() {
        val cases = listOf(
            "name" to request.copy(name = "   "),
            "name" to request.copy(name = "x".repeat(256)),
            "slug" to request.copy(slug = ""),
            "slug" to request.copy(slug = "Invalid_Slug"),
            "slug" to request.copy(slug = "x".repeat(256)),
            "fromYear" to request.copy(fromYear = null),
        )
        for ((field, invalid) in cases) {
            mockMvc.post(createUrl) {
                with(user("writer").roles("SUPER_ADMIN"))
                with(csrf())
                contentType = MediaType.APPLICATION_JSON
                content = mapper.writeValueAsString(invalid)
            }.andExpect {
                status { isBadRequest() }
                content { contentType("application/problem+json") }
                jsonPath("$.errors.$field.length()", greaterThan(0))
            }
        }
        verifyNoInteractions(service, auditReader)
    }

    @ParameterizedTest
    @ValueSource(
        strings = [
            "", "not-json", "{}",
            """{"name":null,"slug":"2026-s4","fromYear":2026}""",
            """{"name":"2026 S4","slug":"2026-s4"}""",
            """{"name":"2026 S4","slug":"2026-s4","fromYear":2026.5}""",
            """{"name":"2026 S4","slug":"2026-s4","fromYear":"2026"}""",
            """{"name":"2026 S4","slug":"2026-s4","fromYear":true}""",
            """{"name":"2026 S4","slug":"2026-s4","fromYear":2147483648}""",
        ],
    )
    fun `malformed bodies and non integer years are rejected`(body: String) {
        mockMvc.post(createUrl) {
            with(user("writer").roles("SUPER_ADMIN"))
            with(csrf())
            contentType = MediaType.APPLICATION_JSON
            content = body
        }.andExpect {
            status { isBadRequest() }
            content { contentType("application/problem+json") }
        }
        verifyNoInteractions(service, auditReader)
    }

    @ParameterizedTest
    @ValueSource(strings = ["name", "slug"])
    fun `duplicate fields return conflict errors`(field: String) {
        whenever(service.create(tournamentId, request)).thenThrow(SeasonFieldConflictException(field))

        mockMvc.post(createUrl) {
            with(user("writer").roles("SUPER_ADMIN"))
            with(csrf())
            contentType = MediaType.APPLICATION_JSON
            content = mapper.writeValueAsString(request)
        }.andExpect {
            status { isConflict() }
            content { contentType("application/problem+json") }
            jsonPath("$.errors.$field[0]", equalTo("A season with this $field already exists in this tournament"))
        }
        verifyNoInteractions(auditReader)
    }

    @Test
    fun `unknown tournament returns not found for both endpoints`() {
        whenever(service.form(tournamentId)).thenThrow(SeasonTournamentNotFoundException(tournamentId))
        whenever(service.create(tournamentId, request)).thenThrow(SeasonTournamentNotFoundException(tournamentId))

        mockMvc.get(formUrl) {
            with(user("writer").roles("SUPER_ADMIN"))
        }.andExpect { status { isNotFound() } }
        mockMvc.post(createUrl) {
            with(user("writer").roles("SUPER_ADMIN"))
            with(csrf())
            contentType = MediaType.APPLICATION_JSON
            content = mapper.writeValueAsString(request)
        }.andExpect { status { isNotFound() } }
        verifyNoInteractions(auditReader)
    }
}
