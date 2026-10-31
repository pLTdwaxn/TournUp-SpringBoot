package com.abeesmall.tournup.tenancy

import com.abeesmall.tournup.catalog.TenantCatalog
import com.abeesmall.tournup.catalog.TenantDatabaseBinding
import java.util.UUID
import org.springframework.stereotype.Component

@Component
class TenantRegistry(
    private val catalog: TenantCatalog,
) {
    fun requireDatabase(tenantId: UUID): TenantDatabaseBinding =
        catalog.findActiveDatabaseBinding(tenantId, "tournup-api")
            ?: error(
                "No active tournup-api database binding for tenant $tenantId",
            )
}
