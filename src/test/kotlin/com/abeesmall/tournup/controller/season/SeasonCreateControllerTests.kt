package com.abeesmall.tournup.controller.season

import com.abeesmall.tournup.dto.audit.LastAuditData
import com.abeesmall.tournup.dto.season.SeasonData
import com.abeesmall.tournup.dto.season.SeasonNamingContext
import com.abeesmall.tournup.dto.season.SeasonWriteRequest
import com.abeesmall.tournup.model.season.Season
import com.abeesmall.tournup.repository.audit.LatestAuditReader
import com.abeesmall.tournup.security.SecurityConfiguration
import com.abeesmall.tournup.service.ResourceRecord
import com.abeesmall.tournup.service.season.SeasonFieldConflictException
import com.abeesmall.tournup.service.season.SeasonService
import com.abeesmall.tournup.service.season.SeasonTournamentNotFoundException
import com.abeesmall.tournup.support.ResourceWebTestConfiguration
import com.abeesmall.tournup.ui.UI_APPLICATION_HEADER
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
import org.mockito.Mockito.verifyNoInteractions
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

@WebMvcTest(SeasonController::class)
@Import(ResourceWebTestConfiguration::class, SecurityConfiguration::class, com.abeesmall.tournup.ui.resource.UiResourceRegistry::class, com.abeesmall.tournup.manifest.season.SeasonManifestConfiguration::class, com.abeesmall.tournup.manifest.tournament.TournamentManifestConfiguration::class)
class SeasonCreateControllerTests(
    @Autowired private val mockMvc: MockMvc,
    @Autowired private val mapper: JsonMapper,
) {
    @MockitoBean
    private lateinit var service: SeasonService

    @MockitoBean
    private lateinit var auditReader: LatestAuditReader

    private val tournamentId = "winter-open-BCDF"
    private val formUrl = "/api/seasons/create/"
    private val prefilledUrl = "$formUrl?tournamentId=$tournamentId"
    private val createUrl = "/api/seasons/"
    private val namingUrl = "/api/seasons/naming-context/?tournamentId=$tournamentId"
    private val request = SeasonWriteRequest("2026 S4", "2026-s4", 2026, tournamentId)
    private val created = SeasonData(UUID.randomUUID(), tournamentId, "2026 S4", "2026-s4", 2026, nanoId = "BCDF")
    private val createdResponse = ResourceRecord(
        item = created,
        included = mapOf("tournament" to listOf(mapOf("id" to tournamentId, "name" to "Winter Open"))),
    )

    @ParameterizedTest
    @ValueSource(strings = ["SUPER_ADMIN", "CLUB_MANAGER", "TOURNAMENT_MANAGER"])
    fun `writers receive a prefilled editable reference with the same lookup contract`(role: String) {
        whenever(service.createForm( tournamentId)).thenReturn(
            ResourceRecord(
                item = SeasonWriteRequest("", "", null, tournamentId),
                included = mapOf(
                    "tournament" to listOf(mapOf("id" to tournamentId, "name" to "Winter Open")),
                ),
            ),
        )

        mockMvc.get(prefilledUrl) {
            with(user("writer").roles(role))
            header(UI_APPLICATION_HEADER, "tournament-manager")
        }.andExpect {
            status { isOk() }
            jsonPath("$.manifest.operation", equalTo("create"))
            jsonPath("$.manifest.fields.fromYear.dataType", equalTo("integer"))
            jsonPath("$.manifest.fields.fromYear.widget", equalTo("number"))
            jsonPath("$.manifest.fields.name.behaviours[0].type", equalTo("season-name-from-year"))
            jsonPath("$.manifest.fields.slug.behaviours[0].type", equalTo("slugify"))
            jsonPath("$.manifest.commands[0].href", equalTo(createUrl))
            jsonPath("$.manifest.fields.tournamentId.dataType", equalTo("reference"))
            jsonPath("$.manifest.fields.tournamentId.widget", equalTo("record-select"))
            jsonPath("$.manifest.fields.tournamentId.editable", equalTo(true))
            jsonPath("$.manifest.fields.tournamentId.reference.resource", equalTo("tournament"))
            jsonPath("$.manifest.fields.tournamentId.reference.lookupOperation", equalTo("lookup"))
            jsonPath("$.manifest.fields.tournamentId.reference.resolveOperation", equalTo("read"))
            jsonPath("$.item.tournamentId", equalTo(tournamentId.toString()))
            jsonPath("$.included.tournament[0].id", equalTo(tournamentId.toString()))
            jsonPath("$.included.tournament[0].name", equalTo("Winter Open"))
            jsonPath("$.item.name", equalTo(""))
            jsonPath("$.item.slug", equalTo(""))
            jsonPath("$.item.fromYear") { hasJsonPath(); value(null) }
        }
        verifyNoInteractions(auditReader)
    }

    @ParameterizedTest
    @ValueSource(strings = ["SUPER_ADMIN", "CLUB_MANAGER", "TOURNAMENT_MANAGER"])
    fun `writers create a season and receive its read manifest and committed audit`(role: String) {
        val actorId = UUID.randomUUID()
        whenever(service.create(request)).thenReturn(createdResponse)
        whenever(auditReader.findLatest(Season::class.java, listOf(created.internalId))).thenReturn(
            mapOf(created.internalId to LastAuditData(42, Instant.parse("2026-11-10T00:00:00Z"), actorId)),
        )

        mockMvc.post(createUrl) {
            with(user("writer").roles(role))
            with(csrf())
            contentType = MediaType.APPLICATION_JSON
            content = mapper.writeValueAsString(request)
        }.andExpect {
            status { isCreated() }
            content { contentType("application/json") }
            jsonPath("$.manifest.resource", equalTo("season"))
            jsonPath("$.manifest.operation", equalTo("read"))
            jsonPath("$.manifest.layout.type", equalTo("detail"))
            jsonPath("$.manifest.layout.sections[0].fields", contains("tournamentId", "fromYear", "name", "slug"))
            jsonPath("$.manifest.fields.tournamentId.dataType", equalTo("reference"))
            jsonPath("$.manifest.fields.tournamentId.widget", equalTo("display"))
            jsonPath("$.manifest.fields.tournamentId.editable", equalTo(false))
            jsonPath("$.included.tournament[0].id", equalTo(tournamentId.toString()))
            jsonPath("$.included.tournament[0].name", equalTo("Winter Open"))
            jsonPath("$.manifest.fields.fromYear.dataType", equalTo("integer"))
            jsonPath("$.manifest.fields.fromYear.widget", equalTo("display"))
            jsonPath("$.manifest.fields.name.editable", equalTo(false))
            jsonPath("$.manifest.fields.slug.editable", equalTo(false))
            jsonPath("$.manifest.commands[0].href", equalTo("/api/seasons/${created.id}/edit/"))
            jsonPath("$.item.id", equalTo(created.id))
            jsonPath("$.item.tournamentId", equalTo(tournamentId.toString()))
            jsonPath("$.item.name", equalTo(request.name))
            jsonPath("$.item.slug", equalTo(request.slug))
            jsonPath("$.item.fromYear", equalTo(2026))
            jsonPath("$.included.tournament[0].name", equalTo("Winter Open"))
            jsonPath("$.item.lastAudit.revision", equalTo(42))
            jsonPath("$.item.lastAudit.timestamp", equalTo("2026-11-10T00:00:00Z"))
            jsonPath("$.item.lastAudit.actorId", equalTo(actorId.toString()))
        }

        val ordered = inOrder(service, auditReader)
        ordered.verify(service).create(request)
        ordered.verify(auditReader).findLatest(Season::class.java, listOf(created.internalId))
        ordered.verifyNoMoreInteractions()
    }

    @Test
    fun `NanoID collision returns a field conflict`() {
        whenever(service.create(request)).thenThrow(
            com.abeesmall.tournup.service.NanoIdConflictException(RuntimeException("duplicate")),
        )
        mockMvc.post("/api/seasons/") {
            with(user("writer").roles("SUPER_ADMIN"))
            with(csrf())
            contentType = MediaType.APPLICATION_JSON
            content = mapper.writeValueAsString(request)
        }.andExpect {
            status { isConflict() }
            jsonPath("$.errors.nanoId[0]", equalTo("NanoID is already in use. Reload the create form and try again."))
        }
        verifyNoInteractions(auditReader)
    }

    @Test
    fun `missing audit history is explicitly null`() {
        whenever(service.create(request)).thenReturn(createdResponse)
        whenever(auditReader.findLatest(Season::class.java, listOf(created.internalId))).thenReturn(emptyMap())

        mockMvc.post(createUrl) {
            with(user("writer").roles("SUPER_ADMIN"))
            with(csrf())
            contentType = MediaType.APPLICATION_JSON
            content = mapper.writeValueAsString(request)
        }.andExpect {
            status { isCreated() }
            jsonPath("$.item.lastAudit") { hasJsonPath(); value(null) }
        }
    }

    @ParameterizedTest
    @ValueSource(strings = ["PLAYER", "COACH"])
    fun `read only roles cannot open the form fetch naming context or create a season`(role: String) {
        mockMvc.get(formUrl) {
            with(user("reader").roles(role))
            header(UI_APPLICATION_HEADER, "tournament-manager")
        }.andExpect { status { isForbidden() } }
        mockMvc.get(namingUrl) {
            with(user("reader").roles(role))
        }.andExpect { status { isForbidden() } }
        mockMvc.post(createUrl) {
            with(user("reader").roles(role))
            with(csrf())
            contentType = MediaType.APPLICATION_JSON
            content = mapper.writeValueAsString(request)
        }.andExpect { status { isForbidden() } }

        verifyNoInteractions(service, auditReader)
    }

    @Test
    fun `all creation endpoints require authentication`() {
        mockMvc.get(formUrl).andExpect { status { isUnauthorized() } }
        mockMvc.get(namingUrl).andExpect { status { isUnauthorized() } }
        mockMvc.post(createUrl) {
            with(csrf())
            contentType = MediaType.APPLICATION_JSON
            content = mapper.writeValueAsString(request)
        }.andExpect { status { isUnauthorized() } }

        verifyNoInteractions(service, auditReader)
    }

    @Test
    fun `creation requires csrf`() {
        mockMvc.post(createUrl) {
            with(user("writer").roles("SUPER_ADMIN"))
            contentType = MediaType.APPLICATION_JSON
            content = mapper.writeValueAsString(request)
        }.andExpect { status { isForbidden() } }

        verifyNoInteractions(service, auditReader)
    }

    @Test
    fun `invalid fields return field errors before calling the service`() {
        val cases = listOf(
            "name" to request.copy(name = "   "),
            "name" to request.copy(name = "x".repeat(256)),
            "slug" to request.copy(slug = ""),
            "slug" to request.copy(slug = "Invalid_Slug"),
            "slug" to request.copy(slug = "x".repeat(256)),
            "fromYear" to request.copy(fromYear = null),
            "tournamentId" to request.copy(tournamentId = null),
        )
        for ((field, invalid) in cases) {
            mockMvc.post(createUrl) {
                with(user("writer").roles("SUPER_ADMIN"))
                with(csrf())
                contentType = MediaType.APPLICATION_JSON
                content = mapper.writeValueAsString(invalid)
            }.andExpect {
                status { isBadRequest() }
                content { contentType("application/problem+json") }
                jsonPath("$.errors.$field.length()", greaterThan(0))
            }
        }
        verifyNoInteractions(service, auditReader)
    }

    @ParameterizedTest
    @ValueSource(
        strings = [
            "", "not-json", "{}",
            """{"name":null,"slug":"2026-s4","fromYear":2026,"tournamentId":"11111111-1111-4111-8111-111111111111"}""",
            """{"name":"2026 S4","slug":"2026-s4","tournamentId":"11111111-1111-4111-8111-111111111111"}""",
            """{"name":"2026 S4","slug":"2026-s4","fromYear":2026.5,"tournamentId":"11111111-1111-4111-8111-111111111111"}""",
            """{"name":"2026 S4","slug":"2026-s4","fromYear":"2026","tournamentId":"11111111-1111-4111-8111-111111111111"}""",
            """{"name":"2026 S4","slug":"2026-s4","fromYear":true,"tournamentId":"11111111-1111-4111-8111-111111111111"}""",
            """{"name":"2026 S4","slug":"2026-s4","fromYear":2147483648,"tournamentId":"11111111-1111-4111-8111-111111111111"}""",
            """{"name":"2026 S4","slug":"2026-s4","fromYear":2026,"tournamentId":"invalid"}""",
        ],
    )
    fun `malformed bodies and non integer years are rejected`(body: String) {
        mockMvc.post(createUrl) {
            with(user("writer").roles("SUPER_ADMIN"))
            with(csrf())
            contentType = MediaType.APPLICATION_JSON
            content = body
        }.andExpect {
            status { isBadRequest() }
            content { contentType("application/problem+json") }
        }
        verifyNoInteractions(service, auditReader)
    }

    @ParameterizedTest
    @ValueSource(strings = ["name"])
    fun `duplicate fields return conflict errors`(field: String) {
        whenever(service.create(request)).thenThrow(SeasonFieldConflictException(field))

        mockMvc.post(createUrl) {
            with(user("writer").roles("SUPER_ADMIN"))
            with(csrf())
            contentType = MediaType.APPLICATION_JSON
            content = mapper.writeValueAsString(request)
        }.andExpect {
            status { isConflict() }
            content { contentType("application/problem+json") }
            jsonPath("$.errors.$field[0]", equalTo("A season with this $field already exists in this tournament"))
        }
        verifyNoInteractions(auditReader)
    }

    @Test
    fun `unknown tournament returns not found for all creation endpoints`() {
        whenever(service.createForm( tournamentId)).thenThrow(SeasonTournamentNotFoundException(tournamentId))
        whenever(service.namingContext(tournamentId)).thenThrow(SeasonTournamentNotFoundException(tournamentId))
        whenever(service.create(request)).thenThrow(SeasonTournamentNotFoundException(tournamentId))

        mockMvc.get(prefilledUrl) {
            with(user("writer").roles("SUPER_ADMIN"))
            header(UI_APPLICATION_HEADER, "tournament-manager")
        }.andExpect { status { isNotFound() } }
        mockMvc.get(namingUrl) {
            with(user("writer").roles("SUPER_ADMIN"))
        }.andExpect { status { isNotFound() } }
        mockMvc.post(createUrl) {
            with(user("writer").roles("SUPER_ADMIN"))
            with(csrf())
            contentType = MediaType.APPLICATION_JSON
            content = mapper.writeValueAsString(request)
        }.andExpect { status { isNotFound() } }
        verifyNoInteractions(auditReader)
    }

    @Test
    fun `form can be requested without selecting a tournament`() {
        whenever(service.createForm( null)).thenReturn(
            ResourceRecord( item = SeasonWriteRequest("", "", null, null)),
        )

        mockMvc.get(formUrl) {
            with(user("writer").roles("TOURNAMENT_MANAGER"))
            header(UI_APPLICATION_HEADER, "tournament-manager")
        }.andExpect {
            status { isOk() }
            jsonPath("$.manifest.application", equalTo("tournament-manager"))
            jsonPath("$.manifest.fields.tournamentId.required", equalTo(true))
            jsonPath("$.manifest.fields.tournamentId.editable", equalTo(true))
            jsonPath("$.item.tournamentId") { hasJsonPath(); value(null) }
            jsonPath("$.included.length()", equalTo(0))
        }
        verifyNoInteractions(auditReader)
    }

    @Test
    fun `form rejects missing or unknown application and malformed tournament ids`() {
        mockMvc.get(formUrl) {
            with(user("writer").roles("SUPER_ADMIN"))
        }.andExpect { status { isBadRequest() } }
        mockMvc.get(formUrl) {
            with(user("writer").roles("SUPER_ADMIN"))
            header(UI_APPLICATION_HEADER, "unknown-app")
        }.andExpect { status { isBadRequest() } }
        mockMvc.get("$formUrl?tournamentId=invalid") {
            with(user("writer").roles("SUPER_ADMIN"))
            header(UI_APPLICATION_HEADER, "tournament-manager")
        }.andExpect { status { isBadRequest() } }
        verifyNoInteractions(service, auditReader)
    }

    @ParameterizedTest
    @ValueSource(strings = ["SUPER_ADMIN", "CLUB_MANAGER", "TOURNAMENT_MANAGER"])
    fun `writers can refresh naming context for a selected tournament`(role: String) {
        whenever(service.namingContext(tournamentId)).thenReturn(
            SeasonNamingContext(tournamentId, mapOf(2026 to listOf("2026 S1", "2026 S3"))),
        )

        mockMvc.get(namingUrl) {
            with(user("writer").roles(role))
        }.andExpect {
            status { isOk() }
            jsonPath("$.tournamentId", equalTo(tournamentId.toString()))
            jsonPath("$.existingNamesByYear.2026", contains("2026 S1", "2026 S3"))
        }
        verifyNoInteractions(auditReader)
    }

    @Test
    fun `naming context requires a valid tournament id`() {
        for (url in listOf("/api/seasons/naming-context/", "/api/seasons/naming-context/?tournamentId=invalid")) {
            mockMvc.get(url) {
                with(user("writer").roles("SUPER_ADMIN"))
            }.andExpect { status { isBadRequest() } }
        }
        verifyNoInteractions(service, auditReader)
    }
}
