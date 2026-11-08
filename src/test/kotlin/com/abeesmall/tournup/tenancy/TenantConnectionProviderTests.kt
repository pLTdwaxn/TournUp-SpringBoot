package com.abeesmall.tournup.tenancy

import java.sql.Connection
import java.util.UUID
import javax.sql.DataSource
import kotlin.test.assertFailsWith
import kotlin.test.assertSame
import kotlin.test.Test
import org.mockito.Mockito.mock
import org.mockito.Mockito.verify
import org.mockito.Mockito.verifyNoInteractions
import org.mockito.Mockito.verifyNoMoreInteractions
import org.mockito.Mockito.`when` as whenever

class TenantConnectionProviderTests {
    private val tenantDataSources = mock(TenantDataSource::class.java)
    private val bootstrap = mock(DataSource::class.java)
    private val provider = TenantConnectionProvider(
        tenantDataSources,
        bootstrap,
    )

    @Test
    fun `selects the new tenant without accessing the existing database`() {
        val existingTenantId = UUID.randomUUID()
        val newTenantId = UUID.randomUUID()
        val existingDatabase = mock(DataSource::class.java)
        val newDatabase = mock(DataSource::class.java)
        val newConnection = mock(Connection::class.java)

        whenever(tenantDataSources.forTenant(existingTenantId))
            .thenReturn(existingDatabase)
        whenever(tenantDataSources.forTenant(newTenantId))
            .thenReturn(newDatabase)
        whenever(newDatabase.connection).thenReturn(newConnection)

        val connection = provider.getConnection(newTenantId)

        assertSame(newConnection, connection)
        verify(tenantDataSources).forTenant(newTenantId)
        verifyNoMoreInteractions(tenantDataSources)
        verifyNoInteractions(existingDatabase, bootstrap)

        provider.releaseConnection(newTenantId, connection)
        verify(newConnection).close()
    }

    @Test
    fun `rejects a missing tenant without accessing any datasource`() {
        assertFailsWith<IllegalStateException> {
            provider.getConnection(TenantIdentifierResolver.NO_TENANT)
        }

        verifyNoInteractions(tenantDataSources, bootstrap)
    }

    @Test
    fun `does not fall back when tenant datasource lookup fails`() {
        val tenantId = UUID.randomUUID()
        val expectedFailure = IllegalStateException(
            "Tenant datasource unavailable",
        )
        whenever(tenantDataSources.forTenant(tenantId))
            .thenThrow(expectedFailure)

        val failure = assertFailsWith<IllegalStateException> {
            provider.getConnection(tenantId)
        }

        assertSame(expectedFailure, failure)
        verify(tenantDataSources).forTenant(tenantId)
        verifyNoMoreInteractions(tenantDataSources)
        verifyNoInteractions(bootstrap)
    }
}
