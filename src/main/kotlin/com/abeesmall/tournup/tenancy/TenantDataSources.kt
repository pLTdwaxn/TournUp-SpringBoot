package com.abeesmall.tournup.tenancy

import com.abeesmall.tournup.catalog.TenantDatabaseBinding
import com.abeesmall.tournup.credentials.DatabaseCredentialResolver
import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import jakarta.annotation.PreDestroy
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap
import javax.sql.DataSource
import org.springframework.stereotype.Component

@Component
class TenantDataSource(
    private val registry: TenantRegistry,
    private val credentialResolver: DatabaseCredentialResolver,
) {
    private class TenantPool(
        val binding: TenantDatabaseBinding,
        val dataSource: HikariDataSource,
    )

    private val pools = ConcurrentHashMap<UUID, TenantPool>()

    fun forTenant(tenantId: UUID): DataSource {
        val binding = registry.requireDatabase(tenantId)

        val pool = pools.computeIfAbsent(tenantId) {
            TenantPool(binding, createPool(binding))
        }

        check(pool.binding == binding) {
            "Database binding changed for tenant $tenantId; " +
                    "restart the application to rebuild its pool"
        }

        return pool.dataSource
    }

    private fun createPool(
        binding: TenantDatabaseBinding,
    ): HikariDataSource {

        val config = HikariConfig().apply {
            jdbcUrl = binding.toPostgresJdbcUrl()
            username = binding.databaseUsername
            password = credentialResolver.resolvePassword(binding.credentialReference)
            poolName = "tenant-${binding.tenantId}-v${binding.configurationVersion}"
            maximumPoolSize = 5
            minimumIdle = 0
        }

        return HikariDataSource(config)
    }

    @PreDestroy
    fun close() {
        pools.values.forEach { it.dataSource.close() }
        pools.clear()
    }
}