package com.abeesmall.tournup.controller.tournament

import com.abeesmall.tournup.dto.audit.LastAuditData
import com.abeesmall.tournup.model.tournament.HandicapMode
import com.abeesmall.tournup.model.tournament.ParticipantType
import com.abeesmall.tournup.model.tournament.ParticipationMode
import com.abeesmall.tournup.model.tournament.Tournament
import com.abeesmall.tournup.repository.audit.LatestAuditReader
import com.abeesmall.tournup.repository.tournament.TournamentRepository
import com.abeesmall.tournup.security.SecurityConfiguration
import com.abeesmall.tournup.service.tournament.TournamentDetailService

import java.time.Instant
import java.util.Optional
import java.util.UUID
import kotlin.test.assertEquals
import org.hamcrest.Matchers.contains
import org.hamcrest.Matchers.equalTo
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource
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

@WebMvcTest(TournamentDetailController::class)
@Import(SecurityConfiguration::class, TournamentDetailService::class)
class TournamentDetailControllerTests(
    @Autowired private val mockMvc: MockMvc,
) {
    @MockitoBean
    private lateinit var tournaments: TournamentRepository

    @MockitoBean
    private lateinit var auditReader: LatestAuditReader

    @Test
    fun `requires an authenticated session`() {
        mockMvc.get("/api/tournaments/${UUID.randomUUID()}/")
            .andExpect {
                status { isUnauthorized() }
            }
        mockMvc.get("/api/tournaments/winter-open/")
            .andExpect { status { isUnauthorized() } }

        verifyNoInteractions(tournaments, auditReader)
    }

    @Test
    @WithMockUser(roles = ["PLAYER"])
    fun `a non UUID identifier is queried as a slug and returns not found when absent`() {
        mockMvc.get("/api/tournaments/not-a-uuid/")
            .andExpect {
                status { isNotFound() }
            }

        verify(tournaments).findBySlug("not-a-uuid")
        verifyNoMoreInteractions(tournaments)
        verifyNoInteractions(auditReader)
    }

    @Test
    @WithMockUser(roles = ["PLAYER"])
    fun `returns not found without looking up another record`() {
        val id = UUID.randomUUID()
        whenever(tournaments.findById(id))
            .thenReturn(Optional.empty<Tournament>())

        mockMvc.get("/api/tournaments/$id/")
            .andExpect {
                status { isNotFound() }
            }

        verify(tournaments).findById(id)
        verifyNoMoreInteractions(tournaments)
    }

    @Test
    @WithMockUser(roles = ["PLAYER"])
    fun `returns the tournament with its detail presentation`() {
        val tournament = Tournament(
            slug = "winter-open",
            name = "Winter Open",
            description = "A team competition",
            participantType = ParticipantType.TEAM,
            participationMode = ParticipationMode.REGISTRATION,
            handicapMode = HandicapMode.CARRY_FORWARD,
        )
        whenever(tournaments.findById(tournament.id))
            .thenReturn(Optional.of(tournament))

        mockMvc.get("/api/tournaments/${tournament.id}/")
            .andExpect {
                status { isOk() }
                content { contentType("application/json") }

                jsonPath("$.manifest.contractVersion", equalTo(1))
                jsonPath("$.manifest.operation", equalTo("read"))
                jsonPath("$.manifest.title", equalTo("Winter Open"))
                jsonPath("$.manifest.layout.type", equalTo("detail"))
                jsonPath(
                    "$.manifest.layout.sections[0].fields",
                    contains("name", "slug", "description"),
                )
                jsonPath(
                    "$.manifest.layout.sections[1].fields",
                    contains(
                        "participantType",
                        "participationMode",
                        "handicapMode",
                    ),
                )
                jsonPath("$.manifest.fields.name.widget", equalTo("display"))
                jsonPath("$.manifest.fields.name.editable", equalTo(false))

                jsonPath("$.item.id", equalTo(tournament.id.toString()))
                jsonPath("$.item.slug", equalTo("winter-open"))
                jsonPath("$.item.name", equalTo("Winter Open"))
                jsonPath("$.item.description", equalTo("A team competition"))
                jsonPath("$.item.participantType", equalTo("TEAM"))
                jsonPath("$.item.participationMode", equalTo("REGISTRATION"))
                jsonPath("$.item.handicapMode", equalTo("CARRY_FORWARD"))
            }
    }

    @ParameterizedTest
    @CsvSource(
        "SUPER_ADMIN,1",
        "CLUB_MANAGER,1",
        "TOURNAMENT_MANAGER,1",
        "PLAYER,0",
        "COACH,0",
    )
    fun `detail exposes edit only to writer roles`(
        role: String,
        expectedCommands: Int,
    ) {
        val tournament = Tournament(
            slug = "winter-open",
            name = "Winter Open",
        )
        whenever(tournaments.findById(tournament.id))
            .thenReturn(Optional.of(tournament))

        mockMvc.get("/api/tournaments/${tournament.id}/") {
            with(user("viewer").roles(role))
        }.andExpect {
            status { isOk() }
            jsonPath("$.manifest.commands.length()", equalTo(expectedCommands))

            if (expectedCommands == 1) {
                jsonPath("$.manifest.commands[0].id", equalTo("edit"))
                jsonPath("$.manifest.commands[0].method", equalTo("GET"))
                jsonPath(
                    "$.manifest.commands[0].href",
                    equalTo("/api/tournaments/${tournament.id}/edit/"),
                )
            }
        }

        verify(tournaments).findById(tournament.id)
        verifyNoMoreInteractions(tournaments)
    }

    @ParameterizedTest
    @ValueSource(
        strings = ["SUPER_ADMIN", "CLUB_MANAGER", "TOURNAMENT_MANAGER"],
    )
    fun `permitted roles receive latest audit metadata`(role: String) {
        val tournament = Tournament(slug = "winter-open", name = "Winter Open")
        val timestamp = Instant.parse("2026-11-10T00:00:00Z")
        val actorId = UUID.randomUUID()

        whenever(tournaments.findById(tournament.id))
            .thenReturn(Optional.of(tournament))
        whenever(
            auditReader.findLatest(
                Tournament::class.java,
                listOf(tournament.id),
            ),
        ).thenReturn(
            mapOf(
                tournament.id to LastAuditData(
                    revision = 42,
                    timestamp = timestamp,
                    actorId = actorId,
                ),
            ),
        )

        mockMvc.get("/api/tournaments/${tournament.id}/") {
            with(user("viewer").roles(role))
        }.andExpect {
            status { isOk() }
            jsonPath("$.item.lastAudit.revision", equalTo(42))
            jsonPath(
                "$.item.lastAudit.timestamp",
                equalTo("2026-11-10T00:00:00Z"),
            )
            jsonPath(
                "$.item.lastAudit.actorId",
                equalTo(actorId.toString()),
            )
        }

        verify(auditReader).findLatest(
            Tournament::class.java,
            listOf(tournament.id),
        )
        verifyNoMoreInteractions(auditReader)
    }

    @ParameterizedTest
    @ValueSource(strings = ["PLAYER", "COACH"])
    fun `restricted roles receive no audit field or lookup`(role: String) {
        val tournament = Tournament(slug = "winter-open", name = "Winter Open")
        whenever(tournaments.findById(tournament.id))
            .thenReturn(Optional.of(tournament))

        mockMvc.get("/api/tournaments/${tournament.id}/") {
            with(user("viewer").roles(role))
        }.andExpect {
            status { isOk() }
            jsonPath("$.item.id", equalTo(tournament.id.toString()))
            jsonPath("$.item.lastAudit") {
                doesNotHaveJsonPath()
            }
        }

        verifyNoInteractions(auditReader)
    }

    @Test
    @WithMockUser(roles = ["SUPER_ADMIN"])
    fun `permitted record without history has explicit null audit`() {
        val tournament = Tournament(slug = "winter-open", name = "Winter Open")
        whenever(tournaments.findById(tournament.id))
            .thenReturn(Optional.of(tournament))
        whenever(
            auditReader.findLatest(
                Tournament::class.java,
                listOf(tournament.id),
            ),
        ).thenReturn(emptyMap())

        mockMvc.get("/api/tournaments/${tournament.id}/")
            .andExpect {
                status { isOk() }
                jsonPath("$.item.lastAudit") {
                    hasJsonPath()
                    value(null)
                }
            }

        verify(auditReader).findLatest(
            Tournament::class.java,
            listOf(tournament.id),
        )
        verifyNoMoreInteractions(auditReader)
    }

    @Test
    @WithMockUser(roles = ["SUPER_ADMIN"])
    fun `missing record does not trigger an audit lookup`() {
        val id = UUID.randomUUID()
        whenever(tournaments.findById(id))
            .thenReturn(Optional.empty<Tournament>())

        mockMvc.get("/api/tournaments/$id/")
            .andExpect {
                status { isNotFound() }
            }

        verify(tournaments).findById(id)
        verifyNoMoreInteractions(tournaments)
        verifyNoInteractions(auditReader)
    }

    @ParameterizedTest
    @ValueSource(strings = ["SUPER_ADMIN", "CLUB_MANAGER", "TOURNAMENT_MANAGER", "PLAYER", "COACH"])
    fun `slug and UUID queries return the same detail and permissions`(role: String) {
        val tournament = Tournament(slug = "winter-open", name = "Winter Open")
        whenever(tournaments.findById(tournament.id)).thenReturn(Optional.of(tournament))
        whenever(tournaments.findBySlug(tournament.slug)).thenReturn(tournament)
        val canViewAudit = role in setOf("SUPER_ADMIN", "CLUB_MANAGER", "TOURNAMENT_MANAGER")
        if (canViewAudit) {
            whenever(auditReader.findLatest(Tournament::class.java, listOf(tournament.id))).thenReturn(
                mapOf(
                    tournament.id to LastAuditData(
                        42, Instant.parse("2026-11-10T00:00:00Z"), UUID.randomUUID(),
                    ),
                ),
            )
        }

        val byId = mockMvc.get("/api/tournaments/${tournament.id}/") {
            with(user("viewer").roles(role))
        }.andExpect { status { isOk() } }.andReturn()
        val bySlug = mockMvc.get("/api/tournaments/${tournament.slug}/") {
            with(user("viewer").roles(role))
        }.andExpect {
            status { isOk() }
            jsonPath("$.item.id", equalTo(tournament.id.toString()))
        }.andReturn()

        assertEquals(byId.response.contentAsString, bySlug.response.contentAsString)
        verify(tournaments).findById(tournament.id)
        verify(tournaments).findBySlug(tournament.slug)
        verifyNoMoreInteractions(tournaments)
        if (canViewAudit) {
            verify(auditReader, times(2)).findLatest(Tournament::class.java, listOf(tournament.id))
            verifyNoMoreInteractions(auditReader)
        } else {
            verifyNoInteractions(auditReader)
        }
    }

    @Test
    @WithMockUser(roles = ["PLAYER"])
    fun `a parsed UUID is never retried as a slug even if a matching slug exists`() {
        val id = UUID.randomUUID()
        whenever(tournaments.findById(id)).thenReturn(Optional.empty())
        whenever(tournaments.findBySlug(id.toString())).thenReturn(
            Tournament(slug = id.toString(), name = "A different record"),
        )

        mockMvc.get("/api/tournaments/$id/").andExpect { status { isNotFound() } }

        verify(tournaments).findById(id)
        verifyNoMoreInteractions(tournaments)
        verifyNoInteractions(auditReader)
    }
}
