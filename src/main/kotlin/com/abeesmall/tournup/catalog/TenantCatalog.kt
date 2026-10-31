package com.abeesmall.tournup.catalog

import java.util.UUID

interface TenantCatalog {

    fun findActiveTenantById(tenantId: UUID): CatalogTenant?

    fun findActiveTenantBySlug(slug: String): CatalogTenant?

    fun findActiveTenantByHostname(hostname: String): CatalogTenant?

    fun findActiveDatabaseBinding(
        tenantId: UUID,
        serviceName: String,
    ): TenantDatabaseBinding?

    fun findActiveDatabaseBindings(
        serviceName: String,
    ): List<TenantDatabaseBinding>
}

data class CatalogTenant(
    val id: UUID,
    val slug: String,
    val name: String,
)

data class TenantDatabaseBinding(
    val tenantId: UUID,
    val serviceName: String,
    val databaseHost: String,
    val databasePort: Int,
    val databaseName: String,
    val databaseUsername: String,
    val credentialReference: String,
    val configurationVersion: Long,
)
