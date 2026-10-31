package com.abeesmall.tournup.auth

import com.abeesmall.tournup.catalog.CatalogTenant
import com.abeesmall.tournup.catalog.TenantCatalog
import com.abeesmall.tournup.security.SecurityConfiguration

import org.mockito.Mockito.`when` as whenever
import org.hamcrest.Matchers.emptyString
import org.hamcrest.Matchers.equalTo
import org.hamcrest.Matchers.not
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest
import org.springframework.context.annotation.Import
import org.springframework.http.MediaType
import org.springframework.mock.web.MockHttpSession
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf
import org.springframework.security.web.context.HttpSessionSecurityContextRepository
import org.springframework.test.context.bean.override.mockito.MockitoBean
import org.springframework.test.context.TestPropertySource
import org.springframework.test.web.servlet.get
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.post
import java.util.UUID
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

@WebMvcTest(AuthController::class)
@Import(
    SecurityConfiguration::class,
    StubAuthAdapter::class,
)
@TestPropertySource(
    properties = ["tournup.auth.provider=stub"],
)
class AuthControllerTests(
    @Autowired private val mockMvc: MockMvc,
) {
    @MockitoBean
    private lateinit var catalog: TenantCatalog

    @BeforeEach
    fun registerKnownTenant() {
        val tenant = CatalogTenant(
            id = UUID.fromString("00000000-0000-0000-0000-000000000002"),
            slug = "demo-club-1",
            name = "Demo Club 1",
        )

        whenever(catalog.findActiveTenantBySlug(tenant.slug))
            .thenReturn(tenant)
    }

    @Test
    fun `auth configuration is publicly available`() {
        mockMvc.get("/api/auth/config/")
            .andExpect {
                status { isOk() }
                content { contentType("application/json") }
                jsonPath("$.provider", equalTo("stub"))
                jsonPath("$.accessLevels.length()", equalTo(5))
                jsonPath("$.accessLevels[0].id", equalTo("SUPER_ADMIN"))
                jsonPath("$.accessLevels[0].label", equalTo("Super Admin"))
            }
    }

    @Test
    fun `stub login creates an authenticated session`() {
        val result = mockMvc.post("/api/auth/login/") {
            with(csrf())
            contentType = MediaType.APPLICATION_JSON
            content = """
                {
                    "credentials": {
                        "accessLevel": "TOURNAMENT_MANAGER",
                        "tenantSlug": "demo-club-1"
                    }
                }
            """.trimIndent()
        }
            .andExpect {
                status { isOk() }
                jsonPath("$.tenantSlug", equalTo("demo-club-1"))
                jsonPath(
                    "$.authorities[0]",
                    equalTo("ROLE_TOURNAMENT_MANAGER"),
                )
            }
            .andReturn()

        val session = assertNotNull(
            result.request.getSession(false)
        ) as MockHttpSession

        assertNotNull(
            session.getAttribute(
                HttpSessionSecurityContextRepository
                    .SPRING_SECURITY_CONTEXT_KEY,
            ),
        )

        mockMvc.get("/api/auth/me/") {
            this.session = session
        }
            .andExpect {
                status { isOk() }
                jsonPath("$.displayName", equalTo("Tournament Manager"))
                jsonPath("$.tenantSlug", equalTo("demo-club-1"))
                jsonPath(
                    "$.authorities[0]",
                    equalTo("ROLE_TOURNAMENT_MANAGER"),
                )
            }

        mockMvc.post("/api/auth/logout/") {
            this.session = session
            with(csrf())
        }
            .andExpect {
                status { isNoContent() }
            }

        assertTrue(session.isInvalid)
    }

    @Test
    fun `csrf token is publicly available`() {
        mockMvc.get("/api/auth/csrf/")
            .andExpect {
                status { isOk() }
                jsonPath(
                    "$.headerName",
                    equalTo("X-CSRF-TOKEN"),
                )
                jsonPath(
                    "$.parameterName",
                    equalTo("_csrf"),
                )
                jsonPath(
                    "$.token",
                    not(emptyString()),
                )
            }
    }

    @Test
    fun `missing access level returns bad request`() {
        mockMvc.post("/api/auth/login/") {
            with(csrf())
            contentType = MediaType.APPLICATION_JSON
            content = """{"credentials":{"tenantSlug":"demo-club-1"}}"""
        }
            .andExpect {
                status { isBadRequest() }
                jsonPath("$.status", equalTo(400))
                jsonPath("$.detail", equalTo("accessLevel is required"))
            }
    }

    @Test
    fun `missing tenant returns bad request`() {
        mockMvc.post("/api/auth/login/") {
            with(csrf())
            contentType = MediaType.APPLICATION_JSON
            content = """{"credentials":{"accessLevel":"PLAYER"}}"""
        }
            .andExpect {
                status { isBadRequest() }
                jsonPath("$.status", equalTo(400))
                jsonPath("$.detail", equalTo("tenantSlug is required"))
            }
    }

    @Test
    fun `unknown tenant returns bad request`() {
        mockMvc.post("/api/auth/login/") {
            with(csrf())
            contentType = MediaType.APPLICATION_JSON
            content = """{"credentials":{"accessLevel":"PLAYER","tenantSlug":"missing"}}"""
        }
            .andExpect {
                status { isBadRequest() }
                jsonPath("$.status", equalTo(400))
                jsonPath(
                    "$.detail",
                    equalTo("Unknown or inactive tenant: missing"),
                )
            }
    }
}
