package com.abeesmall.tournup.auth

import com.abeesmall.tournup.catalog.CatalogTenant
import com.abeesmall.tournup.catalog.TenantCatalog

import java.util.UUID
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.Mockito.mock
import org.mockito.Mockito.`when` as whenever

class StubAuthAdapterTests {

    private val catalog = mock(TenantCatalog::class.java)
    private val adapter = StubAuthAdapter(catalog)

    @BeforeEach
    fun registerKnownTenants() {
        val tenants = listOf(
            CatalogTenant(
                UUID.fromString("00000000-0000-0000-0000-000000000001"),
                "abeesmall",
                "ABeeSmall",
            ),
            CatalogTenant(
                UUID.fromString("00000000-0000-0000-0000-000000000002"),
                "demo-club-1",
                "Demo Club 1",
            ),
        )

        tenants.forEach { tenant ->
            whenever(catalog.findActiveTenantBySlug(tenant.slug))
                .thenReturn(tenant)
        }
    }

    @Test
    fun `provides the selectable access levels`() {
        val configuration = adapter.configuration()

        assertEquals("stub", configuration.provider)
        assertEquals(
            listOf(
                AuthAccessLevel("SUPER_ADMIN", "Super Admin"),
                AuthAccessLevel("PLAYER", "Player"),
                AuthAccessLevel("TOURNAMENT_MANAGER", "Tournament Manager"),
                AuthAccessLevel("CLUB_MANAGER", "Club Manager"),
                AuthAccessLevel("COACH", "Coach"),
            ),
            configuration.accessLevels,
        )
    }

    @Test
    fun `authenticates an explicit club role`() {
        val user = adapter.login(
            AuthLoginRequest(
                credentials = mapOf(
                    "accessLevel" to "TOURNAMENT_MANAGER",
                    "tenantSlug" to "demo-club-1",
                ),
            ),
        )

        assertEquals("demo-club-1", user.tenantSlug)
        assertEquals(
            "00000000-0000-0000-0000-000000000002",
            user.tenantId.toString(),
        )
        assertEquals(setOf("ROLE_TOURNAMENT_MANAGER"), user.authorities)
    }

    @Test
    fun `authenticates an explicit root super admin`() {
        val user = adapter.login(
            AuthLoginRequest(
                credentials = mapOf(
                    "accessLevel" to "SUPER_ADMIN",
                    "tenantSlug" to "abeesmall",
                ),
            ),
        )

        assertEquals("abeesmall", user.tenantSlug)
        assertEquals(
            "00000000-0000-0000-0000-000000000001",
            user.tenantId.toString(),
        )
        assertEquals(setOf("ROLE_SUPER_ADMIN"), user.authorities)
    }

    @Test
    fun `rejects a missing access level`() {
        assertFailsWith<InvalidAuthRequestException> {
            adapter.login(
                AuthLoginRequest(
                    credentials = mapOf("tenantSlug" to "abeesmall"),
                ),
            )
        }
    }

    @Test
    fun `rejects a missing tenant`() {
        assertFailsWith<InvalidAuthRequestException> {
            adapter.login(
                AuthLoginRequest(
                    credentials = mapOf("accessLevel" to "SUPER_ADMIN"),
                ),
            )
        }
    }

    @Test
    fun `uses the tenant identity returned by the catalog`() {
        val tenant = CatalogTenant(
            UUID.fromString("00000000-0000-0000-0000-000000000004"),
            "demo-tenant",
            "Demo tenant",
        )
        whenever(catalog.findActiveTenantBySlug(tenant.slug))
            .thenReturn(tenant)

        val user = adapter.login(
            AuthLoginRequest(
                credentials = mapOf(
                    "accessLevel" to "PLAYER",
                    "tenantSlug" to tenant.slug,
                ),
            ),
        )

        assertEquals(tenant.id, user.tenantId)
        assertEquals(tenant.slug, user.tenantSlug)
        assertEquals(setOf("ROLE_PLAYER"), user.authorities)
    }

    @Test
    fun `rejects a tenant absent from the active catalog`() {
        assertFailsWith<InvalidAuthRequestException> {
            adapter.login(
                AuthLoginRequest(
                    credentials = mapOf(
                        "accessLevel" to "PLAYER",
                        "tenantSlug" to "missing",
                    ),
                ),
            )
        }
    }
}
