package com.abeesmall.tournup.controller.tournament

import com.abeesmall.tournup.dto.tournament.TournamentData
import com.abeesmall.tournup.dto.tournament.TournamentDetailResponse
import com.abeesmall.tournup.dto.tournament.TournamentUpdateFormResponse
import com.abeesmall.tournup.dto.tournament.TournamentUpdateRequest
import com.abeesmall.tournup.manifest.tournament.tournamentDetailManifest
import com.abeesmall.tournup.manifest.tournament.tournamentUpdateManifest
import com.abeesmall.tournup.model.tournament.HandicapMode
import com.abeesmall.tournup.model.tournament.ParticipantType
import com.abeesmall.tournup.model.tournament.ParticipationMode
import com.abeesmall.tournup.security.SecurityConfiguration
import com.abeesmall.tournup.service.tournament.TournamentSlugConflictException
import com.abeesmall.tournup.service.tournament.TournamentUpdateService
import java.util.UUID
import org.hamcrest.Matchers.equalTo
import org.hamcrest.Matchers.greaterThan
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
import org.springframework.http.MediaType
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user
import org.springframework.test.context.bean.override.mockito.MockitoBean
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.get
import org.springframework.test.web.servlet.put
import tools.jackson.databind.json.JsonMapper

@WebMvcTest(TournamentUpdateController::class)
@Import(SecurityConfiguration::class)
class TournamentUpdateControllerTests(
    @Autowired private val mockMvc: MockMvc,
    @Autowired private val mapper: JsonMapper,
) {
    @MockitoBean
    private lateinit var service: TournamentUpdateService

    private val id = UUID.randomUUID()
    private val path = "/api/tournaments/$id/"
    private val request = TournamentUpdateRequest(
        name = "Winter Open",
        slug = "winter-open",
        description = "Updated description",
        participantType = ParticipantType.TEAM,
        participationMode = ParticipationMode.REGISTRATION,
        handicapMode = HandicapMode.CARRY_FORWARD,
    )

    @ParameterizedTest
    @ValueSource(
        strings = ["SUPER_ADMIN", "CLUB_MANAGER", "TOURNAMENT_MANAGER"],
    )
    fun `writers can open the form and update the tournament`(role: String) {
        whenever(service.form(id)).thenReturn(
            TournamentUpdateFormResponse(
                manifest = tournamentUpdateManifest(id),
                item = request,
            ),
        )
        whenever(service.update(id, request)).thenReturn(
            TournamentDetailResponse(
                manifest = tournamentDetailManifest(
                    title = request.name,
                    editHref = "${path}edit/",
                ),
                item = TournamentData(
                    id = id,
                    name = request.name,
                    slug = request.slug,
                    description = request.description,
                    participantType = request.participantType,
                    participationMode = request.participationMode,
                    handicapMode = request.handicapMode,
                ),
            ),
        )

        mockMvc.get("${path}edit/") {
            with(user("writer").roles(role))
        }.andExpect {
            status { isOk() }
            jsonPath("$.manifest.operation", equalTo("update"))
            jsonPath("$.manifest.layout.type", equalTo("form"))
            jsonPath("$.manifest.fields.name.editable", equalTo(true))
            jsonPath("$.manifest.fields.slug.behaviours.length()", equalTo(0))
            jsonPath("$.manifest.commands[0].method", equalTo("PUT"))
            jsonPath("$.manifest.commands[0].href", equalTo(path))
            jsonPath("$.item.name", equalTo(request.name))
            jsonPath("$.item.slug", equalTo(request.slug))
        }

        mockMvc.put(path) {
            with(user("writer").roles(role))
            with(csrf())
            contentType = MediaType.APPLICATION_JSON
            content = mapper.writeValueAsString(request)
        }.andExpect {
            status { isOk() }
            jsonPath("$.manifest.operation", equalTo("read"))
            jsonPath("$.manifest.commands[0].href", equalTo("${path}edit/"))
            jsonPath("$.item.id", equalTo(id.toString()))
            jsonPath("$.item.name", equalTo(request.name))
            jsonPath("$.item.slug", equalTo(request.slug))
            jsonPath("$.item.description", equalTo("Updated description"))
            jsonPath("$.item.participantType", equalTo("TEAM"))
            jsonPath("$.item.participationMode", equalTo("REGISTRATION"))
            jsonPath("$.item.handicapMode", equalTo("CARRY_FORWARD"))
        }

        verify(service).form(id)
        verify(service).update(id, request)
        verifyNoMoreInteractions(service)
    }

    @ParameterizedTest
    @ValueSource(strings = ["PLAYER", "COACH"])
    fun `read-only roles cannot open the form or update`(role: String) {
        mockMvc.get("${path}edit/") {
            with(user("reader").roles(role))
        }.andExpect {
            status { isForbidden() }
        }

        mockMvc.put(path) {
            with(user("reader").roles(role))
            with(csrf())
            contentType = MediaType.APPLICATION_JSON
            content = mapper.writeValueAsString(request)
        }.andExpect {
            status { isForbidden() }
        }

        verifyNoInteractions(service)
    }

    @Test
    fun `both endpoints require authentication`() {
        mockMvc.get("${path}edit/").andExpect {
            status { isUnauthorized() }
        }

        mockMvc.put(path) {
            with(csrf())
            contentType = MediaType.APPLICATION_JSON
            content = mapper.writeValueAsString(request)
        }.andExpect {
            status { isUnauthorized() }
        }

        verifyNoInteractions(service)
    }

    @Test
    fun `update requires a CSRF token`() {
        mockMvc.put(path) {
            with(user("writer").roles("SUPER_ADMIN"))
            contentType = MediaType.APPLICATION_JSON
            content = mapper.writeValueAsString(request)
        }.andExpect {
            status { isForbidden() }
        }

        verifyNoInteractions(service)
    }

    @Test
    fun `both endpoints return 404 for a missing tournament`() {
        mockMvc.get("${path}edit/") {
            with(user("writer").roles("SUPER_ADMIN"))
        }.andExpect {
            status { isNotFound() }
        }

        mockMvc.put(path) {
            with(user("writer").roles("SUPER_ADMIN"))
            with(csrf())
            contentType = MediaType.APPLICATION_JSON
            content = mapper.writeValueAsString(request)
        }.andExpect {
            status { isNotFound() }
        }

        verify(service).form(id)
        verify(service).update(id, request)
        verifyNoMoreInteractions(service)
    }

    @Test
    fun `malformed IDs are rejected before calling the service`() {
        val invalidPath = "/api/tournaments/not-a-uuid/"

        mockMvc.get("${invalidPath}edit/") {
            with(user("writer").roles("SUPER_ADMIN"))
        }.andExpect {
            status { isBadRequest() }
        }

        mockMvc.put(invalidPath) {
            with(user("writer").roles("SUPER_ADMIN"))
            with(csrf())
            contentType = MediaType.APPLICATION_JSON
            content = mapper.writeValueAsString(request)
        }.andExpect {
            status { isBadRequest() }
        }

        verifyNoInteractions(service)
    }

    @Test
    fun `invalid fields return field errors without calling the service`() {
        val cases = listOf(
            "name" to request.copy(name = ""),
            "name" to request.copy(name = "   "),
            "name" to request.copy(name = "x".repeat(256)),
            "slug" to request.copy(slug = ""),
            "slug" to request.copy(slug = "Invalid_Slug"),
            "slug" to request.copy(slug = "x".repeat(256)),
        )

        for ((field, invalidRequest) in cases) {
            mockMvc.put(path) {
                with(user("writer").roles("SUPER_ADMIN"))
                with(csrf())
                contentType = MediaType.APPLICATION_JSON
                content = mapper.writeValueAsString(invalidRequest)
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
        strings = ["participantType", "participationMode", "handicapMode"],
    )
    fun `enum fields reject missing null and unknown values`(field: String) {
        val values: Map<String, Any?> = mapOf(
            "name" to request.name,
            "slug" to request.slug,
            "participantType" to "TEAM",
            "participationMode" to "REGISTRATION",
            "handicapMode" to "CARRY_FORWARD",
        )
        val cases = listOf(
            values - field,
            values + (field to null),
            values + (field to "UNKNOWN"),
        )

        for (invalidBody in cases) {
            mockMvc.put(path) {
                with(user("writer").roles("SUPER_ADMIN"))
                with(csrf())
                contentType = MediaType.APPLICATION_JSON
                content = mapper.writeValueAsString(invalidBody)
            }.andExpect {
                status { isBadRequest() }
                content { contentType("application/problem+json") }
            }
        }

        verifyNoInteractions(service)
    }

    @ParameterizedTest
    @ValueSource(strings = ["", "not-json", "{}"])
    fun `invalid bodies are rejected without calling the service`(body: String) {
        mockMvc.put(path) {
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
        whenever(service.update(id, request))
            .thenThrow(TournamentSlugConflictException())

        mockMvc.put(path) {
            with(user("writer").roles("SUPER_ADMIN"))
            with(csrf())
            contentType = MediaType.APPLICATION_JSON
            content = mapper.writeValueAsString(request)
        }.andExpect {
            status { isConflict() }
            content { contentType("application/problem+json") }
            jsonPath("$.status", equalTo(409))
            jsonPath(
                "$.errors.slug[0]",
                equalTo("A tournament with this slug already exists"),
            )
        }

        verify(service).update(id, request)
        verifyNoMoreInteractions(service)
    }
}
