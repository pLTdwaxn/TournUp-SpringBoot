package com.abeesmall.tournup.auth

import org.junit.jupiter.api.Test
import kotlin.test.assertEquals

class StubAuthAdapterTests {

    private val adapter = StubAuthAdapter()

    @Test
    fun `provides the selectable access levels`() {
        val configuration = adapter.configuration()

        assertEquals("stub", configuration.provider)
        assertEquals(
            listOf(
                AuthAccessLevel("PLAYER", "Player"),
                AuthAccessLevel("TOURNAMENT_MANAGER", "Tournament Manager"),
                AuthAccessLevel("CLUB_MANAGER", "Club Manager"),
                AuthAccessLevel("COACH", "Coach"),
            ),
            configuration.accessLevels,
        )
    }

    @Test
    fun `authenticates the selected access level`() {
        val user = adapter.login(
            AuthLoginRequest(
                credentials = mapOf(
                    "accessLevel" to "TOURNAMENT_MANAGER",
                ),
            ),
        )

        assertEquals("demo", user.tenantSlug)
        assertEquals(
            setOf("ROLE_TOURNAMENT_MANAGER"),
            user.authorities,
        )
    }
}
