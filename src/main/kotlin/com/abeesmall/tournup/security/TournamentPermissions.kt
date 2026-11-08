package com.abeesmall.tournup.security

import org.springframework.security.core.Authentication

internal val tournamentWriterRoles = setOf(
    "SUPER_ADMIN",
    "CLUB_MANAGER",
    "TOURNAMENT_MANAGER",
)

private val tournamentWriterAuthorities =
    tournamentWriterRoles.map { "ROLE_$it" }.toSet()

internal fun canManageTournaments(authentication: Authentication): Boolean =
    authentication.isAuthenticated &&
            authentication.authorities.any {
                it.authority in tournamentWriterAuthorities
            }
