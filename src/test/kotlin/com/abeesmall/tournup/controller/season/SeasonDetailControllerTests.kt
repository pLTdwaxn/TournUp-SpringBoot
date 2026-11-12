package com.abeesmall.tournup.controller.season

import com.abeesmall.tournup.dto.audit.LastAuditData
import com.abeesmall.tournup.model.season.Season
import com.abeesmall.tournup.model.tournament.Tournament
import com.abeesmall.tournup.repository.audit.LatestAuditReader
import com.abeesmall.tournup.repository.season.SeasonRepository
import com.abeesmall.tournup.repository.tournament.TournamentRepository
import com.abeesmall.tournup.security.SecurityConfiguration
import com.abeesmall.tournup.service.season.SeasonService
import com.abeesmall.tournup.support.ResourceWebTestConfiguration
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

@WebMvcTest(SeasonController::class)
@Import(ResourceWebTestConfiguration::class, SecurityConfiguration::class, com.abeesmall.tournup.ui.resource.UiResourceRegistry::class, com.abeesmall.tournup.manifest.season.SeasonManifestConfiguration::class, com.abeesmall.tournup.manifest.tournament.TournamentManifestConfiguration::class, SeasonService::class)
class SeasonDetailControllerTests(
    @Autowired private val mockMvc: MockMvc,
) {
    @MockitoBean
    private lateinit var seasons: SeasonRepository

    @MockitoBean
    private lateinit var auditReader: LatestAuditReader

    @MockitoBean
    private lateinit var tournaments: TournamentRepository

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
    @WithMockUser(roles = ["SUPER_ADMIN"])
    fun `retired nested create form cannot fall through to a slug lookup`() {
        for (parent in listOf(tournament.id.toString(), tournament.slug)) {
            mockMvc.get("/api/tournaments/$parent/seasons/create/").andExpect {
                // The merged controller's PUT item mapping matches this path shape.
                status { isMethodNotAllowed() }
            }
        }
        verifyNoInteractions(seasons, tournaments, auditReader)
    }

    @Test
    fun `requires authentication before looking up the season`() {
        mockMvc.get(url).andExpect { status { isUnauthorized() } }
        mockMvc.get(nestedUrl).andExpect { status { isUnauthorized() } }
        mockMvc.get("/api/tournaments/winter-open/seasons/2026-s1/")
            .andExpect { status { isUnauthorized() } }
        verifyNoInteractions(seasons, tournaments, auditReader)
    }

    @Test
    @WithMockUser(roles = ["PLAYER"])
    fun `a shallow slug query fails before any persistence access`() {
        mockMvc.get("/api/seasons/${season.slug}/").andExpect {
            status { isBadRequest() }
            content { contentType("application/problem+json") }
            jsonPath("$.detail", equalTo("Tournament context is required for a slug lookup"))
        }
        verifyNoInteractions(seasons, tournaments, auditReader)
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
            jsonPath("$.manifest.layout.sections[0].fields", contains("tournamentId", "fromYear", "name", "slug"))
            jsonPath("$.manifest.fields.tournamentId.label", equalTo("Tournament"))
            jsonPath("$.manifest.fields.tournamentId.dataType", equalTo("reference"))
            jsonPath("$.manifest.fields.tournamentId.widget", equalTo("display"))
            jsonPath("$.manifest.fields.tournamentId.editable", equalTo(false))
            jsonPath("$.manifest.fields.tournamentId.required") { doesNotHaveJsonPath() }
            jsonPath("$.context") { doesNotHaveJsonPath() }
            jsonPath("$.manifest.fields.tournamentId.reference.resource", equalTo("tournament"))
            jsonPath("$.manifest.fields.tournamentId.reference.lookupOperation", equalTo("lookup"))
            jsonPath("$.manifest.fields.tournamentId.reference.resolveOperation", equalTo("read"))
            jsonPath("$.included.tournament[0].id", equalTo(tournament.id.toString()))
            jsonPath("$.included.tournament[0].name", equalTo("Winter Open"))
            jsonPath("$.manifest.fields.fromYear.dataType", equalTo("integer"))
            jsonPath("$.manifest.fields.fromYear.widget", equalTo("display"))
            jsonPath("$.manifest.fields.fromYear.editable", equalTo(false))
            jsonPath("$.manifest.fields.name.editable", equalTo(false))
            jsonPath("$.manifest.fields.slug.editable", equalTo(false))
            val canUpdate = role in setOf("SUPER_ADMIN", "CLUB_MANAGER", "TOURNAMENT_MANAGER")
            jsonPath("$.manifest.commands.length()", equalTo(if (canUpdate) 1 else 0))
            if (canUpdate) {
                jsonPath("$.manifest.commands[0].id", equalTo("edit"))
                jsonPath("$.manifest.commands[0].method", equalTo("GET"))
                jsonPath("$.manifest.commands[0].href", equalTo("/api/seasons/${season.id}/edit/"))
            }
            jsonPath("$.item.id", equalTo(season.id.toString()))
            jsonPath("$.item.tournamentId", equalTo(tournament.id.toString()))
            jsonPath("$.item.name", equalTo("2026 S1"))
            jsonPath("$.item.slug", equalTo("2026-s1"))
            jsonPath("$.item.fromYear", equalTo(2026))
            jsonPath("$.included.tournament[0].name", equalTo("Winter Open"))
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

    @Test
    @WithMockUser(roles = ["PLAYER"])
    fun `a season slug is resolved only inside its tournament`() {
        whenever(seasons.findByTournamentIdAndSlug(tournament.id, season.slug)).thenReturn(season)

        mockMvc.get("/api/tournaments/${tournament.id}/seasons/${season.slug}/").andExpect {
            status { isOk() }
            jsonPath("$.item.id", equalTo(season.id.toString()))
            jsonPath("$.item.tournamentId", equalTo(tournament.id.toString()))
            jsonPath("$.included.tournament[0].name", equalTo("Winter Open"))
            jsonPath("$.item.lastAudit") { doesNotHaveJsonPath() }
        }

        verify(seasons).findByTournamentIdAndSlug(tournament.id, season.slug)
        verifyNoMoreInteractions(seasons)
        verifyNoInteractions(tournaments, auditReader)
    }

    @ParameterizedTest
    @ValueSource(strings = ["uuid", "slug"])
    @WithMockUser(roles = ["SUPER_ADMIN"])
    fun `tournament slug context supports season UUID and slug queries`(kind: String) {
        whenever(tournaments.findBySlug(tournament.slug)).thenReturn(tournament)
        val identifier = if (kind == "uuid") {
            whenever(seasons.findById(season.id)).thenReturn(Optional.of(season))
            season.id.toString()
        } else {
            whenever(seasons.findByTournamentIdAndSlug(tournament.id, season.slug)).thenReturn(season)
            season.slug
        }
        val actorId = UUID.randomUUID()
        whenever(auditReader.findLatest(Season::class.java, listOf(season.id))).thenReturn(
            mapOf(season.id to LastAuditData(42, Instant.parse("2026-11-10T00:00:00Z"), actorId)),
        )

        mockMvc.get("/api/tournaments/${tournament.slug}/seasons/$identifier/").andExpect {
            status { isOk() }
            jsonPath("$.item.id", equalTo(season.id.toString()))
            jsonPath("$.item.tournamentId", equalTo(tournament.id.toString()))
            jsonPath("$.item.lastAudit.revision", equalTo(42))
            jsonPath("$.item.lastAudit.actorId", equalTo(actorId.toString()))
            jsonPath("$.manifest.commands[0].href", equalTo("/api/seasons/${season.id}/edit/"))
        }

        verify(tournaments).findBySlug(tournament.slug)
        verifyNoMoreInteractions(tournaments)
        if (kind == "uuid") {
            verify(seasons).findById(season.id)
        } else {
            verify(seasons).findByTournamentIdAndSlug(tournament.id, season.slug)
        }
        verifyNoMoreInteractions(seasons)
        verify(auditReader).findLatest(Season::class.java, listOf(season.id))
        verifyNoMoreInteractions(auditReader)
    }

    @Test
    @WithMockUser(roles = ["SUPER_ADMIN"])
    fun `a missing tournament slug returns not found without querying seasons`() {
        mockMvc.get("/api/tournaments/unknown-tournament/seasons/${season.slug}/")
            .andExpect { status { isNotFound() } }

        verify(tournaments).findBySlug("unknown-tournament")
        verifyNoMoreInteractions(tournaments)
        verifyNoInteractions(seasons, auditReader)
    }

    @Test
    @WithMockUser(roles = ["SUPER_ADMIN"])
    fun `a season slug in another tournament does not fall back to an unscoped query`() {
        val otherId = UUID.randomUUID()

        mockMvc.get("/api/tournaments/$otherId/seasons/${season.slug}/")
            .andExpect { status { isNotFound() } }

        verify(seasons).findByTournamentIdAndSlug(otherId, season.slug)
        verifyNoMoreInteractions(seasons)
        verifyNoInteractions(tournaments, auditReader)
    }

    @Test
    @WithMockUser(roles = ["SUPER_ADMIN"])
    fun `a nested season UUID is never retried as a slug when its ID is absent`() {
        whenever(seasons.findById(season.id)).thenReturn(Optional.empty())
        whenever(seasons.findByTournamentIdAndSlug(tournament.id, season.id.toString())).thenReturn(season)

        mockMvc.get(nestedUrl).andExpect { status { isNotFound() } }

        verify(seasons).findById(season.id)
        verifyNoMoreInteractions(seasons)
        verifyNoInteractions(tournaments, auditReader)
    }

    @ParameterizedTest
    @ValueSource(strings = ["uuid", "slug"])
    @WithMockUser(roles = ["PLAYER"])
    fun `the same season slug in different tournaments resolves to each tournament's record`(context: String) {
        val otherTournament = Tournament(slug = "summer-open", name = "Summer Open")
        val otherSeason = Season(
            tournament = otherTournament,
            name = "Summer season",
            slug = season.slug,
            fromYear = 2026,
        )
        whenever(seasons.findByTournamentIdAndSlug(tournament.id, season.slug)).thenReturn(season)
        whenever(seasons.findByTournamentIdAndSlug(otherTournament.id, season.slug)).thenReturn(otherSeason)
        if (context == "slug") {
            whenever(tournaments.findBySlug(tournament.slug)).thenReturn(tournament)
            whenever(tournaments.findBySlug(otherTournament.slug)).thenReturn(otherTournament)
        }

        for (expected in listOf(season, otherSeason)) {
            val parent = expected.tournament
            val parentIdentifier = if (context == "slug") parent.slug else parent.id.toString()

            mockMvc.get("/api/tournaments/$parentIdentifier/seasons/${season.slug}/").andExpect {
                status { isOk() }
                jsonPath("$.item.id", equalTo(expected.id.toString()))
                jsonPath("$.item.name", equalTo(expected.name))
                jsonPath("$.item.tournamentId", equalTo(parent.id.toString()))
                jsonPath("$.included.tournament[0].name", equalTo(parent.name))
                jsonPath("$.item.lastAudit") { doesNotHaveJsonPath() }
            }
            verify(seasons).findByTournamentIdAndSlug(parent.id, season.slug)
            if (context == "slug") {
                verify(tournaments).findBySlug(parent.slug)
            }
        }
        verifyNoMoreInteractions(seasons)
        if (context == "slug") {
            verifyNoMoreInteractions(tournaments)
        } else {
            verifyNoInteractions(tournaments)
        }
        verifyNoInteractions(auditReader)
    }

    @Test
    @WithMockUser(roles = ["SUPER_ADMIN"])
    fun `a UUID shaped parent context is not retried as a tournament slug`() {
        val parentId = UUID.randomUUID()
        // A different Tournament can have this UUID-shaped slug. ID parsing still wins.
        whenever(tournaments.findBySlug(parentId.toString())).thenReturn(tournament)
        whenever(seasons.findByTournamentIdAndSlug(tournament.id, season.slug)).thenReturn(season)

        mockMvc.get("/api/tournaments/$parentId/seasons/${season.slug}/")
            .andExpect { status { isNotFound() } }

        verify(seasons).findByTournamentIdAndSlug(parentId, season.slug)
        verifyNoMoreInteractions(seasons)
        verifyNoInteractions(tournaments, auditReader)
    }
}
