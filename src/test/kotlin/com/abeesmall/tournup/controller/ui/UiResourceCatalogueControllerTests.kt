package com.abeesmall.tournup.controller.ui

import com.abeesmall.tournup.auth.AuthenticatedUser
import com.abeesmall.tournup.manifest.SeasonManifestConfiguration
import com.abeesmall.tournup.repository.audit.LatestAuditReader
import com.abeesmall.tournup.security.SecurityConfiguration
import com.abeesmall.tournup.service.season.SeasonService
import com.abeesmall.tournup.service.tournament.TournamentService
import com.abeesmall.tournup.service.ui.UiResourceCatalogueService
import com.abeesmall.tournup.support.ResourceWebTestConfiguration
import com.abeesmall.tournup.ui.UI_APPLICATION_HEADER
import com.abeesmall.tournup.ui.UiApplication
import com.abeesmall.tournup.ui.UiManifestResolver
import com.abeesmall.tournup.ui.resource.UiResourceRegistry
import java.util.UUID
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue
import org.hamcrest.Matchers.contains
import org.hamcrest.Matchers.equalTo
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest
import org.springframework.context.annotation.Import
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user
import org.springframework.test.context.bean.override.mockito.MockitoBean
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.get

@WebMvcTest(UiResourceCatalogueController::class)
@Import(
    SecurityConfiguration::class,
    com.abeesmall.tournup.ui.resource.UiResourceRegistry::class,
    SeasonManifestConfiguration::class,
    com.abeesmall.tournup.manifest.tournament.TournamentManifestConfiguration::class,
    ResourceWebTestConfiguration::class,
    UiResourceCatalogueService::class,
    SeasonManifestConfiguration::class,
    com.abeesmall.tournup.manifest.tournament.TournamentManifestConfiguration::class,
)
class UiResourceCatalogueControllerTests(
    @Autowired private val mockMvc: MockMvc,
    @Autowired private val manifests: UiManifestResolver,
) {
    @MockitoBean
    private lateinit var tournaments: TournamentService

    @MockitoBean
    private lateinit var seasons: SeasonService

    @MockitoBean
    private lateinit var audits: LatestAuditReader

    private val path = "/api/ui/resources/"
    private val tenantId = UUID.randomUUID()
    private val userId = UUID.randomUUID()

    @Test
    fun `audit capability requires explicit registration and user permission`() {
        for (audited in listOf(false, true)) {
            val registration =
                com.abeesmall.tournup.manifest.tournament.tournamentResourceDefinition().copy(audited = audited)
            val service = UiResourceCatalogueService(UiResourceRegistry(listOf(registration)), manifests)
            for (role in listOf("PLAYER", "SUPER_ADMIN")) {
                val authentication = viewer(role)
                val catalogue = service.catalogue(
                    UiApplication.TOURNAMENT_MANAGER,
                    authentication,
                    authentication.principal as AuthenticatedUser,
                )
                assertEquals(audited && role == "SUPER_ADMIN", catalogue.resources.single().canReadLastAudit)
            }
        }
    }

    @Test
    fun `requires an authenticated session`() {
        mockMvc.get(path) {
            header(UI_APPLICATION_HEADER, "tournament-manager")
        }.andExpect { status { isUnauthorized() } }
    }

    @Test
    fun `requires a tenant identity rather than an arbitrary authenticated principal`() {
        mockMvc.get(path) {
            with(user("reader").roles("PLAYER"))
            header(UI_APPLICATION_HEADER, "tournament-manager")
        }.andExpect { status { isForbidden() } }
    }

    @Test
    fun `requires an explicit application header`() {
        mockMvc.get(path) {
            with(authentication(viewer()))
        }.andExpect { status { isBadRequest() } }
    }

    @ParameterizedTest
    @ValueSource(strings = ["", "unknown", "tournup-web", "player-portal"])
    fun `rejects unregistered application IDs`(application: String) {
        mockMvc.get(path) {
            with(authentication(viewer()))
            header(UI_APPLICATION_HEADER, application)
        }.andExpect { status { isBadRequest() } }
    }

    @Test
    fun `catalogue supplies approved resources fields relationships and query context`() {
        mockMvc.get(path) {
            with(authentication(viewer()))
            header(UI_APPLICATION_HEADER, "tournament-manager")
        }.andExpect {
            status { isOk() }
            jsonPath("$.contractVersion", equalTo(1))
            jsonPath("$.application", equalTo("tournament-manager"))
            jsonPath("$.scope.tenantId", equalTo(tenantId.toString()))
            jsonPath("$.scope.userId", equalTo(userId.toString()))
            jsonPath("$.resources[*].key", contains("season", "tournament"))
            jsonPath("$.resources[*].pluralName", contains("seasons", "tournaments"))
            jsonPath("$.resources[0].label", equalTo("Seasons"))
            jsonPath("$.resources[0].displayField", equalTo("name"))
            jsonPath("$.resources[0].fields.length()", equalTo(4))
            jsonPath("$.resources[0].fields.tournamentId.dataType", equalTo("reference"))
            jsonPath("$.resources[0].fields.tournamentId.reference.resource", equalTo("tournament"))
            jsonPath("$.resources[0].fields.tournamentId.reference.lookupOperation", equalTo("lookup"))
            jsonPath("$.resources[0].fields.tournamentId.reference.resolveOperation", equalTo("read"))
            jsonPath("$.resources[0].fields.fromYear.dataType", equalTo("integer"))
            jsonPath("$.resources[0].fields.fromYear.required", equalTo(true))
            jsonPath("$.resources[0].fields.name.maxLength", equalTo(255))
            jsonPath("$.resources[0].identifier.field", equalTo("id"))
            jsonPath("$.resources[0].identifier.slugContext.length()", equalTo(0))
            jsonPath("$.resources[0].identifier.type", equalTo("slug-nano-id"))
            jsonPath("$.resources[0].identifier.slugField", equalTo("slug"))
            jsonPath("$.resources[0].identifier.nanoIdField", equalTo("nanoId"))
            jsonPath("$.resources[0].relationships[0].field", equalTo("tournamentId"))
            jsonPath("$.resources[0].relationships[0].resource", equalTo("tournament"))
            jsonPath("$.resources[0].operations[0].href", equalTo("/api/seasons/"))
            jsonPath("$.resources[0].operations[0].requiredContext.length()", equalTo(0))
            jsonPath("$.resources[1].fields.length()", equalTo(6))
            jsonPath("$.resources[1].fields.participantType.dataType", equalTo("enum"))
            jsonPath("$.resources[1].fields.participantType.options[*].value", contains("PLAYER", "TEAM"))
            jsonPath("$.resources[1].operations[0].href", equalTo("/api/tournaments/"))
            jsonPath("$.resources[1].identifier.slugContext.length()", equalTo(0))
            jsonPath("$.resources[1].identifier.type", equalTo("slug-nano-id"))
            jsonPath("$.resources[1].identifier.slugField", equalTo("slug"))
            jsonPath("$.resources[1].identifier.nanoIdField", equalTo("nanoId"))
        }
    }

    @ParameterizedTest
    @ValueSource(strings = ["SUPER_ADMIN", "CLUB_MANAGER", "TOURNAMENT_MANAGER"])
    fun `writers receive create and update links and audit capability`(role: String) {
        mockMvc.get(path) {
            with(authentication(viewer(role)))
            header(UI_APPLICATION_HEADER, "tournament-manager")
        }.andExpect {
            status { isOk() }
            jsonPath("$.resources[0].canReadLastAudit", equalTo(true))
            jsonPath("$.resources[1].canReadLastAudit", equalTo(true))
            jsonPath(
                "$.resources[0].operations[*].id",
                contains(
                    "list",
                    "list-in-tournament",
                    "read",
                    "create-form",
                    "create",
                    "edit-form",
                    "update",
                    "naming-context",
                    "lookup"
                ),
            )
            jsonPath("$.resources[0].operations[3].href", equalTo("/api/seasons/create/"))
            jsonPath("$.resources[0].operations[3].requiredContext.length()", equalTo(0))
            jsonPath("$.resources[0].operations[4].href", equalTo("/api/seasons/"))
            jsonPath("$.resources[0].operations[7].requiredContext", contains("tournamentId"))
            jsonPath("$.resources[0].operations[4].method", equalTo("POST"))
            jsonPath("$.resources[0].operations[6].method", equalTo("PUT"))
            jsonPath(
                "$.resources[1].operations[*].id",
                contains("list", "read", "create-form", "create", "edit-form", "update", "lookup"),
            )
        }
    }

    @ParameterizedTest
    @ValueSource(strings = ["PLAYER", "COACH"])
    fun `readers receive only query links and no audit capability`(role: String) {
        mockMvc.get(path) {
            with(authentication(viewer(role)))
            header(UI_APPLICATION_HEADER, "tournament-manager")
        }.andExpect {
            status { isOk() }
            jsonPath("$.resources[0].canReadLastAudit", equalTo(false))
            jsonPath("$.resources[1].canReadLastAudit", equalTo(false))
            jsonPath("$.resources[0].operations[*].id", contains("list", "list-in-tournament", "read", "lookup"))
            jsonPath("$.resources[1].operations[*].id", contains("list", "read", "lookup"))
        }
    }

    @Test
    fun `catalogue uses a private revalidating cache with stable ETag and supports 304`() {
        val first = mockMvc.get(path) {
            with(authentication(viewer()))
            header(UI_APPLICATION_HEADER, "tournament-manager")
        }.andExpect { status { isOk() } }.andReturn()
        val etag = assertNotNull(first.response.getHeader("ETag"))
        assertEquals("private, no-cache", first.response.getHeader("Cache-Control"))
        val vary = first.response.getHeaders("Vary").flatMap { it.split(',').map(String::trim) }
        assertTrue(UI_APPLICATION_HEADER in vary)
        assertTrue("Cookie" in vary)
        assertTrue("Authorization" in vary)

        val cached = mockMvc.get(path) {
            with(authentication(viewer()))
            header(UI_APPLICATION_HEADER, "tournament-manager")
            header("If-None-Match", etag)
        }.andExpect { status { isNotModified() } }.andReturn()

        assertEquals(etag, cached.response.getHeader("ETag"))
        assertEquals("", cached.response.contentAsString)
        assertEquals("private, no-cache", cached.response.getHeader("Cache-Control"))
    }

    @ParameterizedTest
    @ValueSource(strings = ["user", "tenant", "permissions"])
    fun `a cached catalogue is not reused after its identity or permissions change`(changed: String) {
        val first = mockMvc.get(path) {
            with(authentication(viewer()))
            header(UI_APPLICATION_HEADER, "tournament-manager")
        }.andReturn()
        val oldEtag = assertNotNull(first.response.getHeader("ETag"))
        val nextViewer = when (changed) {
            "user" -> viewer(viewerId = UUID.randomUUID())
            "tenant" -> viewer(viewerTenant = UUID.randomUUID())
            else -> viewer("CLUB_MANAGER")
        }

        val refreshed = mockMvc.get(path) {
            with(authentication(nextViewer))
            header(UI_APPLICATION_HEADER, "tournament-manager")
            header("If-None-Match", oldEtag)
        }.andExpect { status { isOk() } }.andReturn()

        assertNotEquals(oldEtag, refreshed.response.getHeader("ETag"))
    }

    private fun viewer(
        role: String = "PLAYER",
        viewerId: UUID = userId,
        viewerTenant: UUID = tenantId,
    ): UsernamePasswordAuthenticationToken {
        val authorities = setOf("ROLE_$role")
        val principal = AuthenticatedUser(viewerId, "Test user", viewerTenant, "test-club", authorities)
        return UsernamePasswordAuthenticationToken(principal, null, authorities.map(::SimpleGrantedAuthority))
    }
}
