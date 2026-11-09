package com.abeesmall.tournup.controller.tournament

import com.abeesmall.tournup.model.tournament.HandicapMode
import com.abeesmall.tournup.model.tournament.ParticipantType
import com.abeesmall.tournup.model.tournament.ParticipationMode
import com.abeesmall.tournup.model.tournament.Tournament
import com.abeesmall.tournup.repository.tournament.TournamentRepository
import com.abeesmall.tournup.security.SecurityConfiguration
import com.abeesmall.tournup.service.tournament.TournamentListService
import java.util.UUID
import org.hamcrest.Matchers.contains
import org.hamcrest.Matchers.equalTo
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import org.mockito.Mockito.`when` as whenever
import org.mockito.Mockito.verifyNoInteractions
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

    @Test
    fun `requires an authenticated session`() {
        mockMvc.get("/api/tournaments/")
            .andExpect {
                status { isUnauthorized() }
            }

        verifyNoInteractions(tournaments)
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

        verifyNoInteractions(tournaments)
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
}
