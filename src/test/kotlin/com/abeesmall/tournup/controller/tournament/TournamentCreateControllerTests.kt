package com.abeesmall.tournup.controller.tournament

import com.abeesmall.tournup.dto.audit.LastAuditData
import com.abeesmall.tournup.dto.tournament.TournamentCreateRequest
import com.abeesmall.tournup.dto.tournament.TournamentData
import com.abeesmall.tournup.manifest.SeasonManifestConfiguration
import com.abeesmall.tournup.manifest.TournamentManifestConfiguration
import com.abeesmall.tournup.model.tournament.HandicapMode
import com.abeesmall.tournup.model.tournament.ParticipantType
import com.abeesmall.tournup.model.tournament.ParticipationMode
import com.abeesmall.tournup.model.tournament.Tournament
import com.abeesmall.tournup.repository.audit.LatestAuditReader
import com.abeesmall.tournup.security.SecurityConfiguration
import com.abeesmall.tournup.service.ResourceRecord
import com.abeesmall.tournup.service.tournament.TournamentService
import com.abeesmall.tournup.service.tournament.TournamentSlugConflictException
import com.abeesmall.tournup.support.ResourceWebTestConfiguration
import com.abeesmall.tournup.validation.RESOURCE_NAME_MAX_LENGTH
import com.abeesmall.tournup.validation.RESOURCE_SLUG_PATTERN
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
import org.mockito.Mockito.verify
import org.mockito.Mockito.verifyNoInteractions
import org.mockito.Mockito.verifyNoMoreInteractions
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

@WebMvcTest(TournamentController::class)
@Import(
    ResourceWebTestConfiguration::class,
    SecurityConfiguration::class,
    com.abeesmall.tournup.ui.resource.UiResourceRegistry::class,
    SeasonManifestConfiguration::class,
    TournamentManifestConfiguration::class
)
class TournamentCreateControllerTests(
    @Autowired private val mockMvc: MockMvc,
    @Autowired private val mapper: JsonMapper,
) {
    @MockitoBean
    private lateinit var auditReader: LatestAuditReader

    @MockitoBean
    private lateinit var service: TournamentService

    private val validRequest = TournamentCreateRequest(
        name = "Winter Open",
        slug = "winter-open",
        description = "A team competition",
        participantType = ParticipantType.TEAM,
        participationMode = ParticipationMode.REGISTRATION,
        handicapMode = HandicapMode.CARRY_FORWARD,
    )

    private val validBody = """
        {
            "name": "Winter Open",
            "slug": "winter-open",
            "description": "A team competition",
            "participantType": "TEAM",
            "participationMode": "REGISTRATION",
            "handicapMode": "CARRY_FORWARD"
        }
    """.trimIndent()

    @Test
    fun `NanoID collision returns a field conflict`() {
        whenever(service.create(validRequest)).thenThrow(
            com.abeesmall.tournup.service.NanoIdConflictException(RuntimeException("duplicate")),
        )
        mockMvc.post("/api/tournaments/") {
            with(user("writer").roles("SUPER_ADMIN"))
            with(csrf())
            contentType = MediaType.APPLICATION_JSON
            content = mapper.writeValueAsString(validRequest)
        }.andExpect {
            status { isConflict() }
            jsonPath("$.errors.nanoId[0]", equalTo("NanoID is already in use. Reload the create form and try again."))
        }
        verifyNoInteractions(auditReader)
    }

    @Test
    fun `readers are rejected before malformed JSON is parsed`() {
        mockMvc.post("/api/tournaments/") {
            with(user("reader").roles("PLAYER"))
            with(csrf())
            contentType = MediaType.APPLICATION_JSON
            content = "not-json"
        }.andExpect { status { isForbidden() } }
        verifyNoInteractions(service, auditReader)
    }

    @Test
    fun `creation form requires authentication`() {
        mockMvc.get("/api/tournaments/create/")
            .andExpect {
                status { isUnauthorized() }
            }

        verifyNoInteractions(service)
    }

    @ParameterizedTest
    @ValueSource(strings = ["PLAYER", "COACH"])
    fun `read-only roles cannot open the creation form`(role: String) {
        mockMvc.get("/api/tournaments/create/") {
            with(user("reader").roles(role))
        }.andExpect {
            status { isForbidden() }
        }

        verifyNoInteractions(service)
    }

    @ParameterizedTest
    @ValueSource(
        strings = ["SUPER_ADMIN", "CLUB_MANAGER", "TOURNAMENT_MANAGER"],
    )
    fun `writers receive the creation form and defaults`(role: String) {
        mockMvc.get("/api/tournaments/create/") {
            with(user("writer").roles(role))
        }.andExpect {
            status { isOk() }
            content { contentType("application/json") }

            jsonPath("$.manifest.operation", equalTo("create"))
            jsonPath("$.manifest.layout.type", equalTo("form"))
            jsonPath("$.manifest.fields.name.widget", equalTo("text"))
            jsonPath("$.manifest.fields.name.required", equalTo(true))
            jsonPath("$.manifest.fields.name.editable", equalTo(true))
            jsonPath(
                "$.manifest.fields.name.maxLength",
                equalTo(RESOURCE_NAME_MAX_LENGTH),
            )
            jsonPath(
                "$.manifest.fields.slug.pattern",
                equalTo(RESOURCE_SLUG_PATTERN),
            )
            jsonPath("$.manifest.fields.description.widget", equalTo("textarea"))
            jsonPath("$.manifest.fields.participantType.widget", equalTo("select"))
            jsonPath(
                "$.manifest.fields.participantType.options[*].value",
                contains("PLAYER", "TEAM"),
            )
            jsonPath("$.manifest.commands[0].method", equalTo("POST"))
            jsonPath("$.manifest.commands[0].href", equalTo("/api/tournaments/"))

            jsonPath("$.item.nanoId", org.hamcrest.Matchers.matchesPattern("[23456789BCDFGHJKLMNPQRSTVWXYZ]{4}"))
            jsonPath("$.item.name", equalTo(""))
            jsonPath("$.item.slug", equalTo(""))
            jsonPath("$.item.participantType", equalTo("PLAYER"))
            jsonPath("$.item.participationMode", equalTo("WALK_IN"))
            jsonPath("$.item.handicapMode", equalTo("NO_HANDICAP"))
        }

        verifyNoInteractions(service)
    }

    @ParameterizedTest
    @ValueSource(
        strings = ["SUPER_ADMIN", "CLUB_MANAGER", "TOURNAMENT_MANAGER"],
    )
    fun `writers can create a tournament`(role: String) {
        val id = UUID.randomUUID()
        val response = ResourceRecord(
            item = TournamentData(
                internalId = id,
                slug = validRequest.slug,
                name = validRequest.name,
                description = validRequest.description,
                participantType = validRequest.participantType,
                participationMode = validRequest.participationMode,
                handicapMode = validRequest.handicapMode, nanoId = "BCDF"
            ),
        )
        whenever(service.create(validRequest)).thenReturn(response)
        val actorId = UUID.randomUUID()
        whenever(auditReader.findLatest(Tournament::class.java, listOf(id))).thenReturn(
            mapOf(id to LastAuditData(42, Instant.parse("2026-11-12T00:00:00Z"), actorId)),
        )

        mockMvc.post("/api/tournaments/") {
            with(user("writer").roles(role))
            with(csrf())
            contentType = MediaType.APPLICATION_JSON
            content = validBody
        }.andExpect {
            status { isCreated() }
            header { string("Location", "/api/tournaments/winter-open-BCDF/") }
            jsonPath("$.manifest.operation", equalTo("read"))
            jsonPath("$.item.id", equalTo("winter-open-BCDF"))
            jsonPath("$.item.name", equalTo("Winter Open"))
            jsonPath("$.item.slug", equalTo("winter-open"))
            jsonPath("$.item.handicapMode", equalTo("CARRY_FORWARD"))
            jsonPath("$.item.lastAudit.revision", equalTo(42))
            jsonPath("$.item.lastAudit.timestamp", equalTo("2026-11-12T00:00:00Z"))
            jsonPath("$.item.lastAudit.actorId", equalTo(actorId.toString()))
        }

        val ordered = inOrder(service, auditReader)
        ordered.verify(service).create(validRequest)
        ordered.verify(auditReader).findLatest(Tournament::class.java, listOf(id))
        ordered.verifyNoMoreInteractions()
    }

    @ParameterizedTest
    @ValueSource(strings = ["PLAYER", "COACH"])
    fun `read-only roles cannot create tournaments`(role: String) {
        mockMvc.post("/api/tournaments/") {
            with(user("reader").roles(role))
            with(csrf())
            contentType = MediaType.APPLICATION_JSON
            content = validBody
        }.andExpect {
            status { isForbidden() }
        }

        verifyNoInteractions(service)
    }

    @Test
    fun `submission requires authentication`() {
        mockMvc.post("/api/tournaments/") {
            with(csrf())
            contentType = MediaType.APPLICATION_JSON
            content = validBody
        }.andExpect {
            status { isUnauthorized() }
        }

        verifyNoInteractions(service)
    }

    @Test
    fun `submission requires a CSRF token`() {
        mockMvc.post("/api/tournaments/") {
            with(user("writer").roles("SUPER_ADMIN"))
            contentType = MediaType.APPLICATION_JSON
            content = validBody
        }.andExpect {
            status { isForbidden() }
        }

        verifyNoInteractions(service)
    }

    @Test
    fun `invalid fields return errors without calling the service`() {
        val cases = listOf(
            "name" to validRequest.copy(name = ""),
            "name" to validRequest.copy(name = "   "),
            "name" to validRequest.copy(name = "x".repeat(256)),
            "slug" to validRequest.copy(slug = ""),
            "slug" to validRequest.copy(slug = "Invalid_Slug"),
            "slug" to validRequest.copy(slug = "x".repeat(256)),
        )

        for ((field, request) in cases) {
            mockMvc.post("/api/tournaments/") {
                with(user("writer").roles("SUPER_ADMIN"))
                with(csrf())
                contentType = MediaType.APPLICATION_JSON
                content = mapper.writeValueAsString(request)
            }.andExpect {
                status { isBadRequest() }
                content { contentType("application/problem+json") }
                jsonPath("$.errors.$field.length()", greaterThan(0))
            }
        }

        verifyNoInteractions(service)
    }

    @ParameterizedTest
    @ValueSource(
        strings = [
            "",
            "not-json",
            "{}",
            """{"name":null,"slug":"winter-open"}""",
            """{"name":"Winter Open","slug":"winter-open","participantType":"UNKNOWN"}""",
        ],
    )
    fun `invalid bodies are rejected without calling the service`(body: String) {
        mockMvc.post("/api/tournaments/") {
            with(user("writer").roles("SUPER_ADMIN"))
            with(csrf())
            contentType = MediaType.APPLICATION_JSON
            content = body
        }.andExpect {
            status { isBadRequest() }
            content { contentType("application/problem+json") }
        }

        verifyNoInteractions(service)
    }

    @Test
    fun `duplicate slugs return a conflict with a field error`() {
        whenever(service.create(validRequest))
            .thenThrow(TournamentSlugConflictException())

        mockMvc.post("/api/tournaments/") {
            with(user("writer").roles("SUPER_ADMIN"))
            with(csrf())
            contentType = MediaType.APPLICATION_JSON
            content = validBody
        }.andExpect {
            status { isConflict() }
            content { contentType("application/problem+json") }
            jsonPath("$.status", equalTo(409))
            jsonPath(
                "$.errors.slug[0]",
                equalTo("A tournament with this slug already exists"),
            )
        }

        verify(service).create(validRequest)
        verifyNoMoreInteractions(service)
    }
}
