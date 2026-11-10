package com.abeesmall.tournup.security

import org.springframework.security.core.Authentication

internal val seasonWriterRoles = setOf(
    "SUPER_ADMIN",
    "CLUB_MANAGER",
    "TOURNAMENT_MANAGER",
)

private val seasonWriterAuthorities = seasonWriterRoles.map { "ROLE_$it" }.toSet()

internal fun canManageSeasons(authentication: Authentication): Boolean =
    authentication.isAuthenticated &&
        authentication.authorities.any { it.authority in seasonWriterAuthorities }
