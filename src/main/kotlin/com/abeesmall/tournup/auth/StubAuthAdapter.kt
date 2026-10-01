package com.abeesmall.tournup.auth

import java.util.UUID
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.stereotype.Component

enum class StubAccessLevel(
    val label: String,
    val authorities: Set<String>,
) {
    PLAYER(
        label = "Player",
        authorities = setOf("ROLE_PLAYER"),
    ),
    TOURNAMENT_MANAGER(
        label = "Tournament Manager",
        authorities = setOf("ROLE_TOURNAMENT_MANAGER"),
    ),
    CLUB_MANAGER(
        label = "Club Manager",
        authorities = setOf("ROLE_CLUB_MANAGER"),
    ),
    COACH(
        label = "Coach",
        authorities = setOf("ROLE_COACH"),
    ),
}

@Component
@ConditionalOnProperty(
    prefix = "tournup.auth",
    name = ["provider"],
    havingValue = "stub",
)

class StubAuthAdapter : AuthAdapter {

    override fun configuration(): AuthConfiguration =
        AuthConfiguration(
            provider = "stub",
            accessLevels = StubAccessLevel.entries.map { level ->
                AuthAccessLevel(
                    id = level.name,
                    label = level.label,
                )
            },
        )

    override fun login(request: AuthLoginRequest): AuthenticatedUser {
        val requestedLevel = request.credentials["accessLevel"]
            ?: throw InvalidAuthRequestException(
                "accessLevel is required",
            )

        val accessLevel = StubAccessLevel.entries.firstOrNull { level ->
            level.name == requestedLevel
        } ?: throw InvalidAuthRequestException(
            "Unsupported accessLevel: $requestedLevel",
        )

        return AuthenticatedUser(
            userId = UUID.fromString(
                "00000000-0000-0000-0000-000000000001",
            ),
            displayName = accessLevel.label,
            tenantId = UUID.fromString(
                "00000000-0000-0000-0000-000000000002",
            ),
            tenantSlug = "demo",
            authorities = accessLevel.authorities,
        )
    }
}
