package com.abeesmall.tournup.auth

import com.abeesmall.tournup.security.SecurityConfiguration
import org.hamcrest.Matchers.emptyString
import org.hamcrest.Matchers.equalTo
import org.hamcrest.Matchers.not
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest
import org.springframework.context.annotation.Import
import org.springframework.http.MediaType
import org.springframework.mock.web.MockHttpSession
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf
import org.springframework.security.web.context.HttpSessionSecurityContextRepository
import org.springframework.test.context.TestPropertySource
import org.springframework.test.web.servlet.get
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.post
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

    @Test
    fun `auth configuration is publicly available`() {
        mockMvc.get("/api/auth/config/")
            .andExpect {
                status { isOk() }
                content { contentType("application/json") }
                jsonPath("$.provider", equalTo("stub"))
                jsonPath("$.accessLevels.length()", equalTo(4))
                jsonPath("$.accessLevels[0].id", equalTo("PLAYER"))
                jsonPath("$.accessLevels[0].label", equalTo("Player"))
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
                        "accessLevel": "TOURNAMENT_MANAGER"
                    }
                }
            """.trimIndent()
        }
            .andExpect {
                status { isOk() }
                jsonPath("$.tenantSlug", equalTo("demo"))
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
                jsonPath("$.tenantSlug", equalTo("demo"))
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
}
