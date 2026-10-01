package com.abeesmall.tournup.auth

import java.util.UUID

interface AuthAdapter {

    fun configuration(): AuthConfiguration

    fun login(request: AuthLoginRequest): AuthenticatedUser
}

data class AuthConfiguration(
    val provider: String,
    val accessLevels: List<AuthAccessLevel>,
)

data class AuthAccessLevel(
    val id: String,
    val label: String,
)

data class AuthLoginRequest(
    val credentials: Map<String, String>,
)

data class AuthenticatedUser(
    val userId: UUID,
    val displayName: String,
    val tenantId: UUID,
    val tenantSlug: String,
    val authorities: Set<String>,
)

class InvalidAuthRequestException(
    message: String,
) : RuntimeException(message)
