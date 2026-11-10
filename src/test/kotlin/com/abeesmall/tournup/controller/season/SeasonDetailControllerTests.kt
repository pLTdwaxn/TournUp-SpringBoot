package com.abeesmall.tournup.controller.season

import com.abeesmall.tournup.dto.audit.LastAuditData
import com.abeesmall.tournup.model.season.Season
import com.abeesmall.tournup.model.tournament.Tournament
import com.abeesmall.tournup.repository.audit.LatestAuditReader
import com.abeesmall.tournup.repository.season.SeasonRepository
import com.abeesmall.tournup.security.SecurityConfiguration
import com.abeesmall.tournup.service.season.SeasonDetailService
import java.time.Instant
import java.util.Optional
import java.util.UUID
import kotlin.test.assertEquals
import org.hamcrest.Matchers.contains
import org.hamcrest.Matchers.equalTo
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
import org.springframework.security.test.context.support.WithMockUser
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user
import org.springframework.test.context.bean.override.mockito.MockitoBean
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.get

@WebMvcTest(SeasonDetailController::class)
@Import(SecurityConfiguration::class, SeasonDetailService::class)
class SeasonDetailControllerTests(
    @Autowired private val mockMvc: MockMvc,
) {
    @MockitoBean
    private lateinit var seasons: SeasonRepository

    @MockitoBean
    private lateinit var auditReader: LatestAuditReader

    private val tournament = Tournament(slug = "winter-open", name = "Winter Open")
    private val season = Season(
        tournament = tournament,
        name = "2026 S1",
        slug = "2026-s1",
        fromYear = 2026,
    )
    private val url = "/api/seasons/${season.id}/"
    private val nestedUrl = "/api/tournaments/${tournament.id}/seasons/${season.id}/"

    @Test
    fun `requires authentication before looking up the season`() {
        mockMvc.get(url).andExpect { status { isUnauthorized() } }
        mockMvc.get(nestedUrl).andExpect { status { isUnauthorized() } }
        verifyNoInteractions(seasons, auditReader)
    }

    @Test
    @WithMockUser(roles = ["PLAYER"])
    fun `malformed ID is rejected before looking up the season`() {
        mockMvc.get("/api/seasons/not-a-uuid/").andExpect { status { isBadRequest() } }
        mockMvc.get("/api/tournaments/not-a-uuid/seasons/${season.id}/")
            .andExpect { status { isBadRequest() } }
        mockMvc.get("/api/tournaments/${tournament.id}/seasons/not-a-uuid/")
            .andExpect { status { isBadRequest() } }
        verifyNoInteractions(seasons, auditReader)
    }

    @Test
    @WithMockUser(roles = ["SUPER_ADMIN"])
    fun `unknown season returns not found without an audit lookup`() {
        whenever(seasons.findById(season.id)).thenReturn(Optional.empty())

        mockMvc.get(url).andExpect { status { isNotFound() } }
        mockMvc.get(nestedUrl).andExpect { status { isNotFound() } }

        verify(seasons, times(2)).findById(season.id)
        verifyNoMoreInteractions(seasons)
        verifyNoInteractions(auditReader)
    }

    @ParameterizedTest
    @ValueSource(strings = ["SUPER_ADMIN", "CLUB_MANAGER", "TOURNAMENT_MANAGER", "PLAYER", "COACH"])
    fun `nested alias returns the same response and audit visibility as the shallow route`(role: String) {
        whenever(seasons.findById(season.id)).thenReturn(Optional.of(season))
        val canViewAudit = role in setOf("SUPER_ADMIN", "CLUB_MANAGER", "TOURNAMENT_MANAGER")
        if (canViewAudit) {
            whenever(auditReader.findLatest(Season::class.java, listOf(season.id))).thenReturn(
                mapOf(
                    season.id to LastAuditData(
                        42, Instant.parse("2026-11-10T00:00:00Z"), UUID.randomUUID(),
                    ),
                ),
            )
        }

        val shallow = mockMvc.get(url) {
            with(user("viewer").roles(role))
        }.andExpect { status { isOk() } }.andReturn()
        val nested = mockMvc.get(nestedUrl) {
            with(user("viewer").roles(role))
        }.andExpect { status { isOk() } }.andReturn()

        assertEquals(shallow.response.contentAsString, nested.response.contentAsString)
        verify(seasons, times(2)).findById(season.id)
        verifyNoMoreInteractions(seasons)
        if (canViewAudit) {
            verify(auditReader, times(2)).findLatest(Season::class.java, listOf(season.id))
            verifyNoMoreInteractions(auditReader)
        } else {
            verifyNoInteractions(auditReader)
        }
    }

    @ParameterizedTest
    @ValueSource(strings = ["SUPER_ADMIN", "CLUB_MANAGER", "TOURNAMENT_MANAGER", "PLAYER", "COACH"])
    fun `a season under a different tournament returns not found before an audit lookup`(role: String) {
        whenever(seasons.findById(season.id)).thenReturn(Optional.of(season))

        mockMvc.get("/api/tournaments/${UUID.randomUUID()}/seasons/${season.id}/") {
            with(user("viewer").roles(role))
        }.andExpect { status { isNotFound() } }

        verify(seasons).findById(season.id)
        verifyNoMoreInteractions(seasons)
        verifyNoInteractions(auditReader)
    }

    @ParameterizedTest
    @ValueSource(strings = ["SUPER_ADMIN", "CLUB_MANAGER", "TOURNAMENT_MANAGER", "PLAYER", "COACH"])
    fun `readers receive a detail manifest and the stored parent context`(role: String) {
        whenever(seasons.findById(season.id)).thenReturn(Optional.of(season))

        mockMvc.get(url) {
            with(user("viewer").roles(role))
        }.andExpect {
            status { isOk() }
            content { contentType("application/json") }
            jsonPath("$.manifest.contractVersion", equalTo(1))
            jsonPath("$.manifest.resource", equalTo("season"))
            jsonPath("$.manifest.operation", equalTo("read"))
            jsonPath("$.manifest.title", equalTo("2026 S1"))
            jsonPath("$.manifest.layout.type", equalTo("detail"))
            jsonPath("$.manifest.layout.sections[0].fields", contains("fromYear", "name", "slug"))
            jsonPath("$.manifest.fields.fromYear.dataType", equalTo("integer"))
            jsonPath("$.manifest.fields.fromYear.widget", equalTo("display"))
            jsonPath("$.manifest.fields.fromYear.editable", equalTo(false))
            jsonPath("$.manifest.fields.name.editable", equalTo(false))
            jsonPath("$.manifest.fields.slug.editable", equalTo(false))
            jsonPath("$.manifest.commands.length()", equalTo(0))
            jsonPath("$.item.id", equalTo(season.id.toString()))
            jsonPath("$.item.tournamentId", equalTo(tournament.id.toString()))
            jsonPath("$.item.name", equalTo("2026 S1"))
            jsonPath("$.item.slug", equalTo("2026-s1"))
            jsonPath("$.item.fromYear", equalTo(2026))
            jsonPath("$.context.tournament.id", equalTo(tournament.id.toString()))
            jsonPath("$.context.tournament.name", equalTo("Winter Open"))
            jsonPath("$.context.tournament.href", equalTo("/api/tournaments/${tournament.id}/"))
            jsonPath("$.context.collectionHref", equalTo("/api/tournaments/${tournament.id}/seasons/"))
        }
        verify(seasons).findById(season.id)
        verifyNoMoreInteractions(seasons)
    }

    @ParameterizedTest
    @ValueSource(strings = ["SUPER_ADMIN", "CLUB_MANAGER", "TOURNAMENT_MANAGER"])
    fun `permitted roles receive latest audit metadata`(role: String) {
        val actorId = UUID.randomUUID()
        whenever(seasons.findById(season.id)).thenReturn(Optional.of(season))
        whenever(auditReader.findLatest(Season::class.java, listOf(season.id))).thenReturn(
            mapOf(season.id to LastAuditData(42, Instant.parse("2026-11-10T00:00:00Z"), actorId)),
        )

        mockMvc.get(url) {
            with(user("viewer").roles(role))
        }.andExpect {
            status { isOk() }
            jsonPath("$.item.lastAudit.revision", equalTo(42))
            jsonPath("$.item.lastAudit.timestamp", equalTo("2026-11-10T00:00:00Z"))
            jsonPath("$.item.lastAudit.actorId", equalTo(actorId.toString()))
        }
        verify(auditReader).findLatest(Season::class.java, listOf(season.id))
        verifyNoMoreInteractions(auditReader)
    }

    @ParameterizedTest
    @ValueSource(strings = ["PLAYER", "COACH"])
    fun `restricted roles receive no audit metadata or lookup`(role: String) {
        whenever(seasons.findById(season.id)).thenReturn(Optional.of(season))

        mockMvc.get(url) {
            with(user("viewer").roles(role))
        }.andExpect {
            status { isOk() }
            jsonPath("$.item.id", equalTo(season.id.toString()))
            jsonPath("$.item.lastAudit") { doesNotHaveJsonPath() }
        }
        verifyNoInteractions(auditReader)
    }

    @Test
    @WithMockUser(roles = ["SUPER_ADMIN"])
    fun `missing audit history is explicitly null`() {
        whenever(seasons.findById(season.id)).thenReturn(Optional.of(season))
        whenever(auditReader.findLatest(Season::class.java, listOf(season.id))).thenReturn(emptyMap())

        mockMvc.get(url).andExpect {
            status { isOk() }
            jsonPath("$.item.lastAudit") {
                hasJsonPath()
                value(null)
            }
        }
    }
}
