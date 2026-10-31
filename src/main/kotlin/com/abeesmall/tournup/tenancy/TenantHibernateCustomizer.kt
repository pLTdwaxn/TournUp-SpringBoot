package com.abeesmall.tournup.tenancy

import org.hibernate.cfg.MultiTenancySettings
import org.springframework.boot.hibernate.autoconfigure.HibernatePropertiesCustomizer
import org.springframework.stereotype.Component

@Component
class TenantHibernateCustomizer(
    private val connectionProvider: TenantConnectionProvider,
    private val identifierResolver: TenantIdentifierResolver,
) : HibernatePropertiesCustomizer {

    override fun customize(
        hibernateProperties: MutableMap<String, Any>,
    ) {
        hibernateProperties[
            MultiTenancySettings.MULTI_TENANT_CONNECTION_PROVIDER
        ] = connectionProvider

        hibernateProperties[
            MultiTenancySettings.MULTI_TENANT_IDENTIFIER_RESOLVER
        ] = identifierResolver
    }
}
