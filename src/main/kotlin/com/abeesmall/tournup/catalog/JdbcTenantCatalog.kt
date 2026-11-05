package com.abeesmall.tournup.catalog

import java.util.Locale
import java.util.UUID
import javax.sql.DataSource
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.jdbc.core.simple.JdbcClient
import org.springframework.stereotype.Repository

@Repository
@ConditionalOnProperty(
    prefix = "tournup.catalog.datasource",
    name = ["url"],
)
class JdbcTenantCatalog(
    @Qualifier("catalogDataSource")
    dataSource: DataSource,
) : TenantCatalog {

    private val jdbc = JdbcClient.create(dataSource)

    override fun findActiveTenantById(tenantId: UUID): CatalogTenant? =
        jdbc.sql(
            """
                SELECT id, slug, name
                FROM public.tenants
                WHERE id = :tenantId
                AND status = 'ACTIVE'
            """.trimIndent(),
        )
            .param("tenantId", tenantId)
            .query(CatalogTenant::class.java)
            .optional()
            .orElse(null)

    override fun findActiveTenantBySlug(slug: String): CatalogTenant? =
        jdbc.sql(
            """
                        SELECT id, slug, name
                        FROM public.tenants
                        WHERE slug = :slug
                        AND status = 'ACTIVE'
                    """.trimIndent(),
        )
            .param("slug", slug)
            .query(CatalogTenant::class.java)
            .optional()
            .orElse(null)

    override fun findActiveTenantByHostname(
        hostname: String,
    ): CatalogTenant? =
        jdbc.sql(
            """
                            SELECT t.id, t.slug, t.name
                            FROM public.tenants t
                            JOIN public.tenant_domains d ON d.tenant_id = t.id
                            WHERE d.hostname = :hostname
                            AND t.status = 'ACTIVE'
                        """.trimIndent(),
        )
            .param(
                "hostname",
                hostname.lowercase(Locale.ROOT).removeSuffix("."),
            )
            .query(CatalogTenant::class.java)
            .optional()
            .orElse(null)

    override fun findActiveDatabaseBinding(tenantId: UUID, serviceName: String): TenantDatabaseBinding? =
        jdbc.sql(
            """
                SELECT
                b.tenant_id,
                b.service_name,
                b.database_host,
                b.database_port,
                b.database_name,
                b.database_username,
                b.credential_reference,
                b.configuration_version
                FROM public.tenant_database_bindings b
                JOIN public.tenants t ON t.id = b.tenant_id
                WHERE b.tenant_id = :tenantId
                AND b.service_name = :serviceName
                AND b.status = 'ACTIVE'
                AND t.status = 'ACTIVE'
            """.trimIndent(),
        )
            .param("tenantId", tenantId)
            .param("serviceName", serviceName)
            .query(TenantDatabaseBinding::class.java)
            .optional()
            .orElse(null)

    override fun findActiveDatabaseBindings(
        serviceName: String,
    ): List<TenantDatabaseBinding> =
        jdbc.sql(
            """
            SELECT
                b.tenant_id,
                b.service_name,
                b.database_host,
                b.database_port,
                b.database_name,
                b.database_username,
                b.credential_reference,
                b.configuration_version
            FROM public.tenant_database_bindings b
            JOIN public.tenants t ON t.id = b.tenant_id
            WHERE b.service_name = :serviceName
              AND b.status = 'ACTIVE'
              AND t.status = 'ACTIVE'
            ORDER BY b.tenant_id
            """.trimIndent(),
        )
            .param("serviceName", serviceName)
            .query(TenantDatabaseBinding::class.java)
            .list()
            .requireNoNulls()
}
