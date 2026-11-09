package com.abeesmall.tournup.controller.tournament

import com.abeesmall.tournup.dto.audit.LastAuditData
import com.abeesmall.tournup.model.tournament.HandicapMode
import com.abeesmall.tournup.model.tournament.ParticipantType
import com.abeesmall.tournup.model.tournament.ParticipationMode
import com.abeesmall.tournup.model.tournament.Tournament
import com.abeesmall.tournup.repository.audit.LatestAuditReader
import com.abeesmall.tournup.repository.tournament.TournamentRepository
import com.abeesmall.tournup.security.SecurityConfiguration
import com.abeesmall.tournup.service.tournament.TournamentListService

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
import org.springframework.security.test.context.support.WithMockUser
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user
import org.springframework.test.context.bean.override.mockito.MockitoBean
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.get

@WebMvcTest(TournamentListController::class)
@Import(SecurityConfiguration::class, TournamentListService::class)
class TournamentListControllerTests(
    @Autowired private val mockMvc: MockMvc,
) {
    @MockitoBean
    private lateinit var tournaments: TournamentRepository

    @MockitoBean
    private lateinit var auditReader: LatestAuditReader

    @Test
    fun `requires an authenticated session`() {
        mockMvc.get("/api/tournaments/")
            .andExpect {
                status { isUnauthorized() }
            }

        verifyNoInteractions(tournaments, auditReader)
    }

    @ParameterizedTest
    @ValueSource(
        strings = [
            "page=-1",
            "size=0",
            "size=101",
            "page=invalid",
            "size=invalid",
        ],
    )
    @WithMockUser(roles = ["SUPER_ADMIN"])
    fun `rejects invalid pagination`(query: String) {
        mockMvc.get("/api/tournaments/?$query")
            .andExpect {
                status { isBadRequest() }
            }

        verifyNoInteractions(tournaments, auditReader)
    }

    @Test
    @WithMockUser(roles = ["SUPER_ADMIN"])
    fun `returns the requested page with its presentation manifest`() {
        val pageable = PageRequest.of(1, 1, Sort.by("name", "id"))
        val tournament = Tournament(
            id = UUID.randomUUID(),
            slug = "winter-open",
            name = "Winter Open",
            description = "A team competition",
            participantType = ParticipantType.TEAM,
            participationMode = ParticipationMode.REGISTRATION,
            handicapMode = HandicapMode.CARRY_FORWARD,
        )
        whenever(tournaments.findAll(pageable)).thenReturn(
            PageImpl(listOf(tournament), pageable, 3L),
        )

        mockMvc.get("/api/tournaments/?page=1&size=1")
            .andExpect {
                status { isOk() }
                content { contentType("application/json") }

                jsonPath("$.manifest.contractVersion", equalTo(1))
                jsonPath("$.manifest.operation", equalTo("list"))
                jsonPath("$.manifest.layout.type", equalTo("table"))
                jsonPath(
                    "$.manifest.layout.columns",
                    contains("name", "participantType", "handicapMode"),
                )
                jsonPath("$.manifest.fields.name.label", equalTo("Name"))
                jsonPath("$.manifest.fields.name.widget", equalTo("display"))
                jsonPath("$.manifest.fields.name.editable", equalTo(false))
                jsonPath(
                    "$.manifest.fields.participantType.options[1].label",
                    equalTo("Team"),
                )

                jsonPath("$.items.length()", equalTo(1))
                jsonPath("$.items[0].id", equalTo(tournament.id.toString()))
                jsonPath("$.items[0].slug", equalTo("winter-open"))
                jsonPath("$.items[0].name", equalTo("Winter Open"))
                jsonPath("$.items[0].description", equalTo("A team competition"))
                jsonPath("$.items[0].participantType", equalTo("TEAM"))
                jsonPath("$.items[0].participationMode", equalTo("REGISTRATION"))
                jsonPath("$.items[0].handicapMode", equalTo("CARRY_FORWARD"))

                jsonPath("$.pagination.page", equalTo(1))
                jsonPath("$.pagination.size", equalTo(1))
                jsonPath("$.pagination.totalElements", equalTo(3))
                jsonPath("$.pagination.totalPages", equalTo(3))
            }
    }

    @Test
    @WithMockUser(roles = ["SUPER_ADMIN"])
    fun `returns the manifest when there are no tournaments`() {
        val pageable = PageRequest.of(0, 20, Sort.by("name", "id"))
        whenever(tournaments.findAll(pageable)).thenReturn(
            PageImpl(emptyList<Tournament>(), pageable, 0L),
        )

        mockMvc.get("/api/tournaments/")
            .andExpect {
                status { isOk() }
                jsonPath("$.manifest.title", equalTo("Tournaments"))
                jsonPath("$.manifest.layout.type", equalTo("table"))
                jsonPath("$.manifest.layout.columns.length()", equalTo(3))
                jsonPath("$.items.length()", equalTo(0))
                jsonPath("$.pagination.page", equalTo(0))
                jsonPath("$.pagination.size", equalTo(20))
                jsonPath("$.pagination.totalElements", equalTo(0))
                jsonPath("$.pagination.totalPages", equalTo(0))
            }
    }

    @ParameterizedTest
    @ValueSource(
        strings = [
            "SUPER_ADMIN",
            "CLUB_MANAGER",
            "TOURNAMENT_MANAGER",
            "PLAYER",
            "COACH",
        ],
    )
    fun `creation command is advertised only to writers`(role: String) {
        val pageable = PageRequest.of(0, 20, Sort.by("name", "id"))
        whenever(tournaments.findAll(pageable)).thenReturn(
            PageImpl(emptyList<Tournament>(), pageable, 0L),
        )
        val canCreate = role in setOf(
            "SUPER_ADMIN",
            "CLUB_MANAGER",
            "TOURNAMENT_MANAGER",
        )

        mockMvc.get("/api/tournaments/") {
            with(user("viewer").roles(role))
        }.andExpect {
            status { isOk() }
            jsonPath(
                "$.manifest.commands.length()",
                equalTo(if (canCreate) 1 else 0),
            )

            if (canCreate) {
                jsonPath("$.manifest.commands[0].id", equalTo("create"))
                jsonPath("$.manifest.commands[0].method", equalTo("GET"))
                jsonPath(
                    "$.manifest.commands[0].href",
                    equalTo("/api/tournaments/create/"),
                )
            }
        }
    }

    @ParameterizedTest
    @ValueSource(
        strings = ["SUPER_ADMIN", "CLUB_MANAGER", "TOURNAMENT_MANAGER"],
    )
    fun `permitted roles receive audit metadata in one batch`(role: String) {
        val first = Tournament(slug = "first", name = "First")
        val second = Tournament(slug = "second", name = "Second")
        val pageable = PageRequest.of(0, 20, Sort.by("name", "id"))
        val timestamp = Instant.parse("2026-11-10T00:00:00Z")
        val actorId = UUID.randomUUID()

        whenever(tournaments.findAll(pageable)).thenReturn(
            PageImpl(listOf(first, second), pageable, 2L),
        )
        whenever(
            auditReader.findLatest(
                Tournament::class.java,
                listOf(first.id, second.id),
            ),
        ).thenReturn(
            mapOf(
                first.id to LastAuditData(
                    revision = 42,
                    timestamp = timestamp,
                    actorId = actorId,
                ),
            ),
        )

        mockMvc.get("/api/tournaments/") {
            with(user("viewer").roles(role))
        }.andExpect {
            status { isOk() }
            jsonPath("$.items[0].lastAudit.revision", equalTo(42))
            jsonPath(
                "$.items[0].lastAudit.timestamp",
                equalTo("2026-11-10T00:00:00Z"),
            )
            jsonPath(
                "$.items[0].lastAudit.actorId",
                equalTo(actorId.toString()),
            )
            jsonPath("$.items[1].lastAudit") {
                hasJsonPath()
                value(null)
            }
        }

        verify(auditReader).findLatest(
            Tournament::class.java,
            listOf(first.id, second.id),
        )
        verifyNoMoreInteractions(auditReader)
    }

    @ParameterizedTest
    @ValueSource(strings = ["PLAYER", "COACH"])
    fun `restricted roles receive no audit field or lookup`(role: String) {
        val tournament = Tournament(slug = "winter-open", name = "Winter Open")
        val pageable = PageRequest.of(0, 20, Sort.by("name", "id"))

        whenever(tournaments.findAll(pageable)).thenReturn(
            PageImpl(listOf(tournament), pageable, 1L),
        )

        mockMvc.get("/api/tournaments/") {
            with(user("viewer").roles(role))
        }.andExpect {
            status { isOk() }
            jsonPath("$.items[0].id", equalTo(tournament.id.toString()))
            jsonPath("$.items[0].lastAudit") {
                doesNotHaveJsonPath()
            }
        }

        verifyNoInteractions(auditReader)
    }
}
