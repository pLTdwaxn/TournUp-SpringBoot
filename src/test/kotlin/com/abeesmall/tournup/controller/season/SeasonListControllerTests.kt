package com.abeesmall.tournup.controller.season

import com.abeesmall.tournup.controller.resource.ResourceCrudController
import com.abeesmall.tournup.dto.audit.LastAuditData
import com.abeesmall.tournup.model.season.Season
import com.abeesmall.tournup.model.tournament.Tournament
import com.abeesmall.tournup.repository.audit.LatestAuditReader
import com.abeesmall.tournup.repository.season.SeasonRepository
import com.abeesmall.tournup.repository.tournament.TournamentRepository
import com.abeesmall.tournup.resource.ResourceRegistry
import com.abeesmall.tournup.resource.season.SeasonResourceHandler
import com.abeesmall.tournup.security.SecurityConfiguration
import com.abeesmall.tournup.service.season.SeasonService
import com.abeesmall.tournup.support.ResourceWebTestConfiguration
import java.time.Instant
import java.util.UUID
import org.hamcrest.Matchers.contains
import org.hamcrest.Matchers.equalTo
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import org.mockito.Mockito.`when` as whenever
import org.mockito.Mockito.verify
import org.mockito.Mockito.verifyNoInteractions
import org.mockito.Mockito.verifyNoMoreInteractions
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest
import org.springframework.context.annotation.Import
import org.springframework.data.domain.PageImpl
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Sort
import org.springframework.http.MediaType
import org.springframework.security.test.context.support.WithMockUser
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user
import org.springframework.test.context.bean.override.mockito.MockitoBean
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.get
import org.springframework.test.web.servlet.post

@WebMvcTest(SeasonController::class, ResourceCrudController::class)
@Import(ResourceWebTestConfiguration::class, SecurityConfiguration::class, ResourceRegistry::class, SeasonResourceHandler::class, SeasonService::class)
class SeasonListControllerTests(
    @Autowired private val mockMvc: MockMvc,
) {
    @MockitoBean
    private lateinit var tournaments: TournamentRepository

    @MockitoBean
    private lateinit var seasons: SeasonRepository

    @MockitoBean
    private lateinit var auditReader: LatestAuditReader

    private val tournament = Tournament(slug = "winter-open", name = "Winter Open")
    private val url = "/api/tournaments/${tournament.id}/seasons/"
    private val sort = Sort.by(
        Sort.Order.desc("fromYear"), Sort.Order.asc("name"), Sort.Order.asc("id"),
    )

    @Test
    @WithMockUser(roles = ["SUPER_ADMIN"])
    fun `nested collection supports listing but rejects creation`() {
        mockMvc.post(url) {
            with(csrf())
            contentType = MediaType.APPLICATION_JSON
            content = """{"tournamentId":"${tournament.id}","name":"2026 S1","slug":"2026-s1","fromYear":2026}"""
        }.andExpect { status { isMethodNotAllowed() } }
        verifyNoInteractions(tournaments, seasons, auditReader)
    }

    @Test
    fun `requires authentication`() {
        mockMvc.get(url).andExpect { status { isUnauthorized() } }
        mockMvc.get("/api/seasons/").andExpect { status { isUnauthorized() } }
        verifyNoInteractions(tournaments, seasons, auditReader)
    }

    @ParameterizedTest
    @ValueSource(strings = ["page=-1", "size=0", "size=101", "page=invalid", "size=invalid"])
    @WithMockUser(roles = ["SUPER_ADMIN"])
    fun `invalid pagination never reaches persistence`(query: String) {
        mockMvc.get("$url?$query").andExpect { status { isBadRequest() } }
        mockMvc.get("/api/seasons/?$query").andExpect { status { isBadRequest() } }
        verifyNoInteractions(tournaments, seasons, auditReader)
    }

    @Test
    @WithMockUser(roles = ["SUPER_ADMIN"])
    fun `unknown tournament returns not found without querying seasons`() {
        whenever(tournaments.existsById(tournament.id)).thenReturn(false)

        mockMvc.get(url).andExpect {
            status { isNotFound() }
            content { contentType("application/problem+json") }
            jsonPath("$.detail", equalTo("Tournament not found"))
        }
        verify(tournaments).existsById(tournament.id)
        verifyNoInteractions(seasons, auditReader)
    }

    @Test
    @WithMockUser(roles = ["SUPER_ADMIN"])
    fun `returns the requested tournament page with its table manifest`() {
        val season = season("2026 S2", "2026-s2", 2026)
        val pageable = givenPage(listOf(season), page = 1, size = 1, total = 3L)

        mockMvc.get("${url}?page=1&size=1").andExpect {
            status { isOk() }
            content { contentType("application/json") }
            jsonPath("$.manifest.contractVersion", equalTo(1))
            jsonPath("$.manifest.resource", equalTo("season"))
            jsonPath("$.manifest.operation", equalTo("list"))
            jsonPath("$.manifest.layout.type", equalTo("table"))
            jsonPath("$.manifest.layout.columns", contains("name"))
            jsonPath("$.manifest.fields.fromYear.dataType", equalTo("integer"))
            jsonPath("$.manifest.fields.fromYear.label", equalTo("From year"))
            jsonPath("$.manifest.fields.fromYear.widget", equalTo("display"))
            jsonPath("$.manifest.fields.fromYear.editable", equalTo(false))
            jsonPath("$.manifest.fields.name.label", equalTo("Name"))
            jsonPath("$.manifest.fields.name.editable", equalTo(false))
            jsonPath("$.manifest.fields.slug.label", equalTo("Slug"))
            jsonPath("$.items.length()", equalTo(1))
            jsonPath("$.items[0].id", equalTo(season.id.toString()))
            jsonPath("$.items[0].tournamentId", equalTo(tournament.id.toString()))
            jsonPath("$.items[0].name", equalTo("2026 S2"))
            jsonPath("$.items[0].slug", equalTo("2026-s2"))
            jsonPath("$.items[0].fromYear", equalTo(2026))
            jsonPath("$.pagination.page", equalTo(1))
            jsonPath("$.pagination.size", equalTo(1))
            jsonPath("$.pagination.totalElements", equalTo(3))
            jsonPath("$.pagination.totalPages", equalTo(3))
        }
        verify(tournaments).existsById(tournament.id)
        verify(seasons).findAllByTournamentId(tournament.id, pageable)
        verifyNoMoreInteractions(tournaments, seasons)
    }

    @Test
    @WithMockUser(roles = ["SUPER_ADMIN"])
    fun `empty tournament still returns the manifest and default pagination`() {
        givenPage(emptyList())

        mockMvc.get(url).andExpect {
            status { isOk() }
            jsonPath("$.manifest.title", equalTo("Seasons"))
            jsonPath("$.manifest.layout.columns", contains("name"))
            jsonPath("$.items.length()", equalTo(0))
            jsonPath("$.pagination.page", equalTo(0))
            jsonPath("$.pagination.size", equalTo(20))
            jsonPath("$.pagination.totalElements", equalTo(0))
            jsonPath("$.pagination.totalPages", equalTo(0))
        }
        verifyNoInteractions(auditReader)
    }

    @Test
    @WithMockUser(roles = ["SUPER_ADMIN"])
    fun `page beyond the end is empty and retains the total`() {
        givenPage(emptyList(), page = 2, size = 1, total = 1L)

        mockMvc.get("${url}?page=2&size=1").andExpect {
            status { isOk() }
            jsonPath("$.items.length()", equalTo(0))
            jsonPath("$.pagination.page", equalTo(2))
            jsonPath("$.pagination.totalElements", equalTo(1))
            jsonPath("$.pagination.totalPages", equalTo(1))
        }
        verifyNoInteractions(auditReader)
    }

    @ParameterizedTest
    @ValueSource(strings = ["SUPER_ADMIN", "CLUB_MANAGER", "TOURNAMENT_MANAGER", "PLAYER", "COACH"])
    fun `readers can list seasons and only writers receive a create command`(role: String) {
        givenPage(emptyList())
        val canCreate = role in setOf("SUPER_ADMIN", "CLUB_MANAGER", "TOURNAMENT_MANAGER")

        mockMvc.get(url) {
            with(user("viewer").roles(role))
        }.andExpect {
            status { isOk() }
            jsonPath("$.manifest.commands.length()", equalTo(if (canCreate) 1 else 0))
            if (canCreate) {
                jsonPath("$.manifest.commands[0].id", equalTo("create"))
                jsonPath("$.manifest.commands[0].method", equalTo("GET"))
                jsonPath("$.manifest.commands[0].href", equalTo("/api/seasons/create/?tournamentId=${tournament.id}"))
            }
        }
    }

    @ParameterizedTest
    @ValueSource(strings = ["SUPER_ADMIN", "CLUB_MANAGER", "TOURNAMENT_MANAGER"])
    fun `permitted roles receive audit metadata in one batch for this page`(role: String) {
        val first = season("2026 S1", "2026-s1", 2026)
        val second = season("2026 S2", "2026-s2", 2026)
        val actorId = UUID.randomUUID()
        givenPage(listOf(first, second))
        whenever(auditReader.findLatest(Season::class.java, listOf(first.id, second.id))).thenReturn(
            mapOf(first.id to LastAuditData(42, Instant.parse("2026-11-10T00:00:00Z"), actorId)),
        )

        mockMvc.get(url) {
            with(user("viewer").roles(role))
        }.andExpect {
            status { isOk() }
            jsonPath("$.items[0].lastAudit.revision", equalTo(42))
            jsonPath("$.items[0].lastAudit.timestamp", equalTo("2026-11-10T00:00:00Z"))
            jsonPath("$.items[0].lastAudit.actorId", equalTo(actorId.toString()))
            jsonPath("$.items[1].lastAudit") {
                hasJsonPath()
                value(null)
            }
        }
        verify(auditReader).findLatest(Season::class.java, listOf(first.id, second.id))
        verifyNoMoreInteractions(auditReader)
    }

    @ParameterizedTest
    @ValueSource(strings = ["PLAYER", "COACH"])
    fun `restricted roles receive neither audit metadata nor an audit lookup`(role: String) {
        val season = season("2026 S1", "2026-s1", 2026)
        givenPage(listOf(season))

        mockMvc.get(url) {
            with(user("viewer").roles(role))
        }.andExpect {
            status { isOk() }
            jsonPath("$.items[0].id", equalTo(season.id.toString()))
            jsonPath("$.items[0].lastAudit") { doesNotHaveJsonPath() }
        }
        verifyNoInteractions(auditReader)
    }

    @ParameterizedTest
    @ValueSource(strings = ["SUPER_ADMIN", "CLUB_MANAGER", "TOURNAMENT_MANAGER", "PLAYER", "COACH"])
    fun `listing without a tournament includes records from different tournaments`(role: String) {
        val canCreate = role in setOf("SUPER_ADMIN", "CLUB_MANAGER", "TOURNAMENT_MANAGER")
        val first = season("2026 S1", "2026-s1", 2026)
        val otherTournament = Tournament(slug = "summer-open", name = "Summer Open")
        val second = Season(
            tournament = otherTournament, name = "2026 S1", slug = "2026-s1", fromYear = 2026,
        )
        val pageable = PageRequest.of(0, 20, sort)
        whenever(seasons.findAll(pageable)).thenReturn(PageImpl(listOf(first, second), pageable, 2L))

        mockMvc.get("/api/seasons/") {
            with(user("viewer").roles(role))
        }.andExpect {
            status { isOk() }
            jsonPath("$.manifest.resource", equalTo("season"))
            jsonPath("$.manifest.operation", equalTo("list"))
            jsonPath("$.manifest.layout.columns", contains("name"))
            jsonPath("$.manifest.commands.length()", equalTo(if (canCreate) 1 else 0))
            if (canCreate) {
                jsonPath("$.manifest.commands[0].href", equalTo("/api/seasons/create/"))
            }
            jsonPath("$.items.length()", equalTo(2))
            jsonPath("$.items[0].id", equalTo(first.id.toString()))
            jsonPath("$.items[0].tournamentId", equalTo(tournament.id.toString()))
            jsonPath("$.items[1].id", equalTo(second.id.toString()))
            jsonPath("$.items[1].tournamentId", equalTo(otherTournament.id.toString()))
            jsonPath("$.pagination.totalElements", equalTo(2))
            jsonPath("$.pagination.totalPages", equalTo(1))
        }

        verify(seasons).findAll(pageable)
        verifyNoMoreInteractions(seasons)
        verifyNoInteractions(tournaments)
        if (role in setOf("SUPER_ADMIN", "CLUB_MANAGER", "TOURNAMENT_MANAGER")) {
            verify(auditReader).findLatest(Season::class.java, listOf(first.id, second.id))
            verifyNoMoreInteractions(auditReader)
        } else {
            verifyNoInteractions(auditReader)
        }
    }

    @Test
    @WithMockUser(roles = ["PLAYER"])
    fun `listing without a tournament preserves pagination and stable sorting`() {
        val record = season("2026 S2", "2026-s2", 2026)
        val pageable = PageRequest.of(1, 1, sort)
        whenever(seasons.findAll(pageable)).thenReturn(PageImpl(listOf(record), pageable, 3L))

        mockMvc.get("/api/seasons/?page=1&size=1").andExpect {
            status { isOk() }
            jsonPath("$.items.length()", equalTo(1))
            jsonPath("$.items[0].id", equalTo(record.id.toString()))
            jsonPath("$.pagination.page", equalTo(1))
            jsonPath("$.pagination.size", equalTo(1))
            jsonPath("$.pagination.totalElements", equalTo(3))
            jsonPath("$.pagination.totalPages", equalTo(3))
        }

        verify(seasons).findAll(pageable)
        verifyNoMoreInteractions(seasons)
        verifyNoInteractions(tournaments, auditReader)
    }

    @Test
    @WithMockUser(roles = ["SUPER_ADMIN"])
    fun `listing without a tournament returns an empty page without an audit lookup`() {
        val pageable = PageRequest.of(0, 20, sort)
        whenever(seasons.findAll(pageable)).thenReturn(PageImpl(emptyList<Season>(), pageable, 0L))

        mockMvc.get("/api/seasons/").andExpect {
            status { isOk() }
            jsonPath("$.manifest.title", equalTo("Seasons"))
            jsonPath("$.manifest.commands.length()", equalTo(1))
            jsonPath("$.manifest.commands[0].href", equalTo("/api/seasons/create/"))
            jsonPath("$.items.length()", equalTo(0))
            jsonPath("$.pagination.totalElements", equalTo(0))
        }

        verify(seasons).findAll(pageable)
        verifyNoMoreInteractions(seasons)
        verifyNoInteractions(tournaments, auditReader)
    }

    private fun givenPage(
        items: List<Season>,
        page: Int = 0,
        size: Int = 20,
        total: Long = items.size.toLong(),
    ): PageRequest {
        val pageable = PageRequest.of(page, size, sort)
        whenever(tournaments.existsById(tournament.id)).thenReturn(true)
        whenever(seasons.findAllByTournamentId(tournament.id, pageable))
            .thenReturn(PageImpl(items, pageable, total))
        return pageable
    }

    private fun season(name: String, slug: String, year: Int): Season =
        Season(tournament = tournament, name = name, slug = slug, fromYear = year)
}
