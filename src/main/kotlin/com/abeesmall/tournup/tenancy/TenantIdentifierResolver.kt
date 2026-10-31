package com.abeesmall.tournup.tenancy

import com.abeesmall.tournup.auth.AuthenticatedUser
import java.util.UUID
import org.hibernate.context.spi.CurrentTenantIdentifierResolver
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.stereotype.Component

@Component
class TenantIdentifierResolver(
    private val registry: TenantRegistry,
) : CurrentTenantIdentifierResolver<UUID> {

    companion object {
        val NO_TENANT: UUID = UUID(0L, 0L)
    }

    override fun resolveCurrentTenantIdentifier(): UUID {
        val authentication = SecurityContextHolder.getContext().authentication

        if (authentication == null || !authentication.isAuthenticated) {
            return NO_TENANT
        }

        val user = authentication.principal as? AuthenticatedUser
            ?: error("An authenticated tenant user is required")

        registry.requireDatabase(user.tenantId)

        return user.tenantId
    }

    override fun validateExistingCurrentSessions(): Boolean = true
}
