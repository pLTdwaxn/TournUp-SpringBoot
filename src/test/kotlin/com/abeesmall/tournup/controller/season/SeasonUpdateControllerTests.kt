package com.abeesmall.tournup.controller.season

import com.abeesmall.tournup.dto.audit.LastAuditData
import com.abeesmall.tournup.dto.season.SeasonUpdateRequest
import com.abeesmall.tournup.model.season.Season
import com.abeesmall.tournup.model.tournament.Tournament
import com.abeesmall.tournup.repository.audit.LatestAuditReader
import com.abeesmall.tournup.repository.season.SeasonRepository
import com.abeesmall.tournup.security.SecurityConfiguration
import com.abeesmall.tournup.service.season.SeasonUpdateService
import java.time.Instant
import java.util.Optional
import java.util.UUID
import kotlin.test.assertEquals
import org.hamcrest.Matchers.contains
import org.hamcrest.Matchers.equalTo
import org.hamcrest.Matchers.greaterThan
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import org.mockito.Mockito.`when` as whenever
import org.mockito.Mockito.times
import org.mockito.Mockito.verify
import org.mockito.Mockito.verifyNoInteractions
import org.mockito.Mockito.verifyNoMoreInteractions
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest
import org.springframework.context.annotation.Import
import org.springframework.http.MediaType
import org.springframework.security.test.context.support.WithMockUser
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user
import org.springframework.test.context.bean.override.mockito.MockitoBean
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.get
import org.springframework.test.web.servlet.put
import tools.jackson.databind.json.JsonMapper

@WebMvcTest(SeasonUpdateController::class)
@Import(SecurityConfiguration::class, SeasonUpdateService::class)
class SeasonUpdateControllerTests(
    @Autowired private val mockMvc: MockMvc,
    @Autowired private val mapper: JsonMapper,
) {
    @MockitoBean
    private lateinit var seasons: SeasonRepository

    @MockitoBean
    private lateinit var auditReader: LatestAuditReader

    private val tournament = Tournament(slug = "winter-open", name = "Winter Open")
    private val existing = Season(
        tournament = tournament, name = "2026 S1", slug = "2026-s1", fromYear = 2026,
    )
    private val request = SeasonUpdateRequest("Summer league", "summer-league", 2027, tournament.id)
    private val shallowUrl = "/api/seasons/${existing.id}/"
    private val urls = listOf(shallowUrl, "/api/tournaments/${tournament.id}/seasons/${existing.id}/")

    @ParameterizedTest
    @ValueSource(strings = ["SUPER_ADMIN", "CLUB_MANAGER", "TOURNAMENT_MANAGER"])
    fun `writers receive manual editing forms through both routes`(role: String) {
        whenever(seasons.findById(existing.id)).thenReturn(Optional.of(existing))

        for (url in urls) {
            mockMvc.get("${url}edit/") {
                with(user("writer").roles(role))
            }.andExpect {
                status { isOk() }
                jsonPath("$.manifest.operation", equalTo("update"))
                jsonPath("$.manifest.layout.type", equalTo("form"))
                jsonPath("$.manifest.layout.sections[0].fields", contains("tournamentId", "fromYear", "name", "slug"))
                jsonPath("$.manifest.fields.tournamentId.dataType", equalTo("reference"))
                jsonPath("$.manifest.fields.tournamentId.widget", equalTo("record-select"))
                jsonPath("$.manifest.fields.tournamentId.required", equalTo(true))
                jsonPath("$.manifest.fields.tournamentId.editable", equalTo(false))
                jsonPath("$.manifest.fields.tournamentId.reference.resource", equalTo("tournament"))
                jsonPath("$.manifest.fields.tournamentId.reference.lookupOperation", equalTo("list"))
                jsonPath("$.manifest.fields.tournamentId.reference.resolveOperation", equalTo("read"))
                jsonPath("$.item.tournamentId", equalTo(tournament.id.toString()))
                jsonPath("$.included.tournament[0].id", equalTo(tournament.id.toString()))
                jsonPath("$.included.tournament[0].name", equalTo("Winter Open"))
                jsonPath("$.manifest.fields.fromYear.widget", equalTo("number"))
                jsonPath("$.manifest.fields.fromYear.required", equalTo(true))
                jsonPath("$.manifest.fields.name.behaviours.length()", equalTo(0))
                jsonPath("$.manifest.fields.slug.behaviours.length()", equalTo(0))
                jsonPath("$.manifest.commands[0].method", equalTo("PUT"))
                jsonPath("$.manifest.commands[0].href", equalTo(shallowUrl))
                jsonPath("$.item.name", equalTo("2026 S1"))
                jsonPath("$.item.slug", equalTo("2026-s1"))
                jsonPath("$.item.fromYear", equalTo(2026))
                jsonPath("$.context.tournament.id", equalTo(tournament.id.toString()))
                jsonPath("$.context.tournament.name", equalTo("Winter Open"))
            }
        }
        verifyNoInteractions(auditReader)
    }

    @ParameterizedTest
    @ValueSource(strings = ["SUPER_ADMIN", "CLUB_MANAGER", "TOURNAMENT_MANAGER"])
    fun `writers update through both routes and receive detail context and audit metadata`(role: String) {
        whenever(seasons.findById(existing.id)).thenReturn(Optional.of(existing))
        whenever(seasons.saveAndFlush(existing)).thenReturn(existing)
        val actorId = UUID.randomUUID()
        whenever(auditReader.findLatest(Season::class.java, listOf(existing.id))).thenReturn(
            mapOf(existing.id to LastAuditData(43, Instant.parse("2026-11-10T00:00:00Z"), actorId)),
        )

        for (url in urls) {
            mockMvc.put(url) {
                with(user("writer").roles(role))
                with(csrf())
                contentType = MediaType.APPLICATION_JSON
                content = mapper.writeValueAsString(request)
            }.andExpect {
                status { isOk() }
                jsonPath("$.manifest.operation", equalTo("read"))
                jsonPath("$.manifest.fields.tournamentId.dataType", equalTo("reference"))
                jsonPath("$.manifest.fields.tournamentId.widget", equalTo("display"))
                jsonPath("$.included.tournament[0].id", equalTo(tournament.id.toString()))
                jsonPath("$.included.tournament[0].name", equalTo("Winter Open"))
                jsonPath("$.manifest.title", equalTo("Summer league"))
                jsonPath("$.manifest.commands[0].href", equalTo("${shallowUrl}edit/"))
                jsonPath("$.item.id", equalTo(existing.id.toString()))
                jsonPath("$.item.tournamentId", equalTo(tournament.id.toString()))
                jsonPath("$.item.name", equalTo("Summer league"))
                jsonPath("$.item.slug", equalTo("summer-league"))
                jsonPath("$.item.fromYear", equalTo(2027))
                jsonPath("$.item.lastAudit.revision", equalTo(43))
                jsonPath("$.item.lastAudit.actorId", equalTo(actorId.toString()))
                jsonPath("$.context.tournament.name", equalTo("Winter Open"))
                jsonPath("$.context.collectionHref", equalTo("/api/tournaments/${tournament.id}/seasons/"))
            }
        }
        verify(seasons, times(2)).saveAndFlush(existing)
        verify(auditReader, times(2)).findLatest(Season::class.java, listOf(existing.id))
    }

    @ParameterizedTest
    @ValueSource(strings = ["PLAYER", "COACH"])
    fun `readers cannot edit or update through either route`(role: String) {
        for (url in urls) {
            mockMvc.get("${url}edit/") {
                with(user("reader").roles(role))
            }.andExpect { status { isForbidden() } }
            mockMvc.put(url) {
                with(user("reader").roles(role))
                with(csrf())
                contentType = MediaType.APPLICATION_JSON
                content = mapper.writeValueAsString(request)
            }.andExpect { status { isForbidden() } }
        }
        verifyNoInteractions(seasons, auditReader)
    }

    @Test
    fun `both routes require authentication`() {
        for (url in urls) {
            mockMvc.get("${url}edit/").andExpect { status { isUnauthorized() } }
            mockMvc.put(url) {
                with(csrf())
                contentType = MediaType.APPLICATION_JSON
                content = mapper.writeValueAsString(request)
            }.andExpect { status { isUnauthorized() } }
        }
        verifyNoInteractions(seasons, auditReader)
    }

    @Test
    @WithMockUser(roles = ["SUPER_ADMIN"])
    fun `both update routes require csrf`() {
        for (url in urls) {
            mockMvc.put(url) {
                contentType = MediaType.APPLICATION_JSON
                content = mapper.writeValueAsString(request)
            }.andExpect { status { isForbidden() } }
        }
        verifyNoInteractions(seasons, auditReader)
    }

    @Test
    @WithMockUser(roles = ["SUPER_ADMIN"])
    fun `unknown season has no form or update and no audit lookup`() {
        whenever(seasons.findById(existing.id)).thenReturn(Optional.empty())
        for (url in urls) {
            mockMvc.get("${url}edit/").andExpect { status { isNotFound() } }
            mockMvc.put(url) {
                with(csrf())
                contentType = MediaType.APPLICATION_JSON
                content = mapper.writeValueAsString(request)
            }.andExpect { status { isNotFound() } }
        }
        verify(seasons, times(4)).findById(existing.id)
        verifyNoMoreInteractions(seasons)
        verifyNoInteractions(auditReader)
    }

    @Test
    @WithMockUser(roles = ["SUPER_ADMIN"])
    fun `wrong tournament returns not found without changing the season`() {
        whenever(seasons.findById(existing.id)).thenReturn(Optional.of(existing))
        val url = "/api/tournaments/${UUID.randomUUID()}/seasons/${existing.id}/"

        mockMvc.get("${url}edit/").andExpect { status { isNotFound() } }
        mockMvc.put(url) {
            with(csrf())
            contentType = MediaType.APPLICATION_JSON
            content = mapper.writeValueAsString(request)
        }.andExpect { status { isNotFound() } }

        assertEquals("2026 S1", existing.name)
        assertEquals("2026-s1", existing.slug)
        assertEquals(2026, existing.fromYear)
        verify(seasons, times(2)).findById(existing.id)
        verifyNoMoreInteractions(seasons)
        verifyNoInteractions(auditReader)
    }

    @Test
    @WithMockUser(roles = ["SUPER_ADMIN"])
    fun `invalid fields return field errors without persistence calls`() {
        val cases = listOf(
            "name" to request.copy(name = "   "),
            "name" to request.copy(name = "x".repeat(256)),
            "slug" to request.copy(slug = ""),
            "slug" to request.copy(slug = "Invalid_Slug"),
            "slug" to request.copy(slug = "x".repeat(256)),
            "fromYear" to request.copy(fromYear = null),
            "tournamentId" to request.copy(tournamentId = null),
        )
        for (url in urls) {
            for ((field, invalid) in cases) {
                mockMvc.put(url) {
                    with(csrf())
                    contentType = MediaType.APPLICATION_JSON
                    content = mapper.writeValueAsString(invalid)
                }.andExpect {
                    status { isBadRequest() }
                    content { contentType("application/problem+json") }
                    jsonPath("$.errors.$field.length()", greaterThan(0))
                }
            }
        }
        verifyNoInteractions(seasons, auditReader)
    }

    @Test
    @WithMockUser(roles = ["SUPER_ADMIN"])
    fun `a different submitted tournament returns a field error through both routes`() {
        whenever(seasons.findById(existing.id)).thenReturn(Optional.of(existing))
        val reparented = request.copy(tournamentId = UUID.randomUUID())

        for (url in urls) {
            mockMvc.put(url) {
                with(csrf())
                contentType = MediaType.APPLICATION_JSON
                content = mapper.writeValueAsString(reparented)
            }.andExpect {
                status { isBadRequest() }
                content { contentType("application/problem+json") }
                jsonPath("$.errors.tournamentId[0]", equalTo("Tournament cannot be changed after creation"))
            }
        }
        assertEquals(tournament.id, existing.tournament.id)
        assertEquals("2026 S1", existing.name)
        assertEquals("2026-s1", existing.slug)
        assertEquals(2026, existing.fromYear)
        verify(seasons, times(2)).findById(existing.id)
        verifyNoMoreInteractions(seasons)
        verifyNoInteractions(auditReader)
    }

    @Test
    @WithMockUser(roles = ["SUPER_ADMIN"])
    fun `omitting tournament is a validation error before persistence access`() {
        for (url in urls) {
            mockMvc.put(url) {
                with(csrf())
                contentType = MediaType.APPLICATION_JSON
                content = """{"name":"Summer league","slug":"summer-league","fromYear":2027}"""
            }.andExpect {
                status { isBadRequest() }
                jsonPath("$.errors.tournamentId[0]", equalTo("Tournament is required"))
            }
        }
        verifyNoInteractions(seasons, auditReader)
    }

    @ParameterizedTest
    @ValueSource(
        strings = [
            "", "not-json", "{}",
            """{"name":"Summer league","slug":"summer-league","tournamentId":"11111111-1111-4111-8111-111111111111"}""",
            """{"name":"Summer league","slug":"summer-league","fromYear":2027.5,"tournamentId":"11111111-1111-4111-8111-111111111111"}""",
            """{"name":"Summer league","slug":"summer-league","fromYear":"2027","tournamentId":"11111111-1111-4111-8111-111111111111"}""",
            """{"name":"Summer league","slug":"summer-league","fromYear":2027,"tournamentId":"invalid"}""",
        ],
    )
    @WithMockUser(roles = ["SUPER_ADMIN"])
    fun `malformed requests and non integer years are rejected`(body: String) {
        for (url in urls) {
            mockMvc.put(url) {
                with(csrf())
                contentType = MediaType.APPLICATION_JSON
                content = body
            }.andExpect {
                status { isBadRequest() }
                content { contentType("application/problem+json") }
            }
        }
        verifyNoInteractions(seasons, auditReader)
    }

    @ParameterizedTest
    @ValueSource(strings = ["name", "slug"])
    @WithMockUser(roles = ["SUPER_ADMIN"])
    fun `conflicts return field errors without an audit lookup`(field: String) {
        whenever(seasons.findById(existing.id)).thenReturn(Optional.of(existing))
        if (field == "name") {
            whenever(seasons.existsByTournamentIdAndNameAndIdNot(tournament.id, request.name, existing.id))
                .thenReturn(true)
        } else {
            whenever(seasons.existsByTournamentIdAndSlugAndIdNot(tournament.id, request.slug, existing.id))
                .thenReturn(true)
        }
        for (url in urls) {
            mockMvc.put(url) {
                with(csrf())
                contentType = MediaType.APPLICATION_JSON
                content = mapper.writeValueAsString(request)
            }.andExpect {
                status { isConflict() }
                content { contentType("application/problem+json") }
                jsonPath("$.errors.$field[0]", equalTo("A season with this $field already exists in this tournament"))
            }
        }
        verifyNoInteractions(auditReader)
    }
}
