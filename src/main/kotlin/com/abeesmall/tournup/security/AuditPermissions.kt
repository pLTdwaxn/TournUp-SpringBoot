package com.abeesmall.tournup.security

import org.springframework.security.core.Authentication

private val lastAuditViewerAuthorities = setOf(
    "ROLE_SUPER_ADMIN",
    "ROLE_CLUB_MANAGER",
    "ROLE_TOURNAMENT_MANAGER",
)

internal fun canViewLastAudit(authentication: Authentication): Boolean =
    authentication.isAuthenticated &&
            authentication.authorities.any {
                it.authority in lastAuditViewerAuthorities
            }
