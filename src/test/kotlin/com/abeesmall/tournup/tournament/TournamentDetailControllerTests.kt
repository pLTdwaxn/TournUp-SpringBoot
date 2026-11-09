package com.abeesmall.tournup.tournament

import com.abeesmall.tournup.security.SecurityConfiguration
import java.util.Optional
import java.util.UUID
import org.hamcrest.Matchers.contains
import org.hamcrest.Matchers.equalTo
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource
import org.mockito.Mockito.`when` as whenever
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

    @Test
    fun `requires an authenticated session`() {
        mockMvc.get("/api/tournaments/${UUID.randomUUID()}/")
            .andExpect {
                status { isUnauthorized() }
            }

        verifyNoInteractions(tournaments)
    }

    @Test
    @WithMockUser(roles = ["PLAYER"])
    fun `rejects a malformed tournament ID`() {
        mockMvc.get("/api/tournaments/not-a-uuid/")
            .andExpect {
                status { isBadRequest() }
            }

        verifyNoInteractions(tournaments)
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
}
