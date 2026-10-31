package com.abeesmall.tournup.tenancy

import java.util.UUID
import javax.sql.DataSource
import org.hibernate.engine.jdbc.connections.spi.AbstractDataSourceBasedMultiTenantConnectionProviderImpl
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.stereotype.Component

@Component
class TenantConnectionProvider(
    private val tenantDataSource: TenantDataSource,
    @param:Qualifier("dataSource")
    private val bootstrapDataSource: DataSource,
) : AbstractDataSourceBasedMultiTenantConnectionProviderImpl<UUID>() {

    protected override fun selectAnyDataSource(): DataSource =
        bootstrapDataSource

    protected override fun selectDataSource(
        tenantIdentifier: UUID,
    ): DataSource {
        check(tenantIdentifier != TenantIdentifierResolver.NO_TENANT) {
            "An authenticated tenant is required for database access"
        }

        return tenantDataSource.forTenant(tenantIdentifier)
    }
}
