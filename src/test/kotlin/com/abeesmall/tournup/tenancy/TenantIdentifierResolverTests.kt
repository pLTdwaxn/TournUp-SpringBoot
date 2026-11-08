package com.abeesmall.tournup.tenancy

import com.abeesmall.tournup.auth.AuthenticatedUser
import com.abeesmall.tournup.catalog.TenantCatalog
import com.abeesmall.tournup.catalog.TenantDatabaseBinding
import java.util.UUID
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Test
import org.mockito.Mockito.mock
import org.mockito.Mockito.`when` as whenever
import org.mockito.Mockito.verify
import org.mockito.Mockito.verifyNoInteractions
import org.mockito.Mockito.verifyNoMoreInteractions
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.context.SecurityContextHolder


class TenantIdentifierResolverTests {
    private val catalog = mock(TenantCatalog::class.java)
    private val resolver = TenantIdentifierResolver(TenantRegistry(catalog))

    @AfterEach
    fun clearSecurityContext() {
        SecurityContextHolder.clearContext()
    }

    @Test
    fun `resolves the current user tenant when switching between users`() {
        val firstTenantId = UUID.randomUUID()
        val secondTenantId = UUID.randomUUID()

        whenever(catalog.findActiveDatabaseBinding(firstTenantId, "tournup-api"))
            .thenReturn(binding(firstTenantId, "first_tournup"))
        whenever(catalog.findActiveDatabaseBinding(secondTenantId, "tournup-api"))
            .thenReturn(binding(secondTenantId, "second_tournup"))

        for (tenantId in listOf(firstTenantId, secondTenantId, firstTenantId)) {
            authenticate(tenantId)

            assertEquals(
                tenantId,
                resolver.resolveCurrentTenantIdentifier(),
            )
        }
    }

    @Test
    fun `returns no tenant without authentication`() {
        SecurityContextHolder.clearContext()

        assertEquals(
            TenantIdentifierResolver.NO_TENANT,
            resolver.resolveCurrentTenantIdentifier(),
        )
        verifyNoInteractions(catalog)
    }

    @Test
    fun `rejects a tenant without an active database binding`() {
        val tenantId = UUID.randomUUID()
        authenticate(tenantId)

        assertFailsWith<IllegalStateException> {
            resolver.resolveCurrentTenantIdentifier()
        }

        verify(catalog).findActiveDatabaseBinding(tenantId, "tournup-api")
        verifyNoMoreInteractions(catalog)
    }

    @Test
    fun `rejects an authenticated principal without a tenant identity`() {
        val context = SecurityContextHolder.createEmptyContext()
        context.authentication = UsernamePasswordAuthenticationToken(
            "generic-user", null, emptyList(),
        )
        SecurityContextHolder.setContext(context)

        assertFailsWith<IllegalStateException> {
            resolver.resolveCurrentTenantIdentifier()
        }

        verifyNoInteractions(catalog)
    }

    private fun authenticate(tenantId: UUID) {
        val user = AuthenticatedUser(
            userId = UUID.randomUUID(),
            displayName = "Test user",
            tenantId = tenantId,
            tenantSlug = "test-$tenantId",
            authorities = emptySet(),
        )
        val context = SecurityContextHolder.createEmptyContext()
        context.authentication = UsernamePasswordAuthenticationToken(
            user, null, emptyList(),
        )
        SecurityContextHolder.setContext(context)
    }

    private fun binding(
        tenantId: UUID,
        databaseName: String,
    ): TenantDatabaseBinding =
        TenantDatabaseBinding(
            tenantId = tenantId,
            serviceName = "tournup-api",
            databaseHost = "database.invalid",
            databasePort = 5432,
            databaseName = databaseName,
            databaseUsername = "${databaseName}_app",
            credentialReference = "env:TEST_DATABASE_PASSWORD",
            configurationVersion = 1L,
        )
}
