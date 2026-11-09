package com.abeesmall.tournup.tournament

import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Pattern
import jakarta.validation.constraints.Size

data class TournamentUpdateRequest(
    @field:NotBlank(message = "Name is required")
    @field:Size(
        max = TOURNAMENT_NAME_MAX_LENGTH,
        message = "Name must be at most {max} characters",
    )
    val name: String,

    @field:NotBlank(message = "Slug is required")
    @field:Size(
        max = TOURNAMENT_SLUG_MAX_LENGTH,
        message = "Slug must be at most {max} characters",
    )
    @field:Pattern(
        regexp = TOURNAMENT_SLUG_PATTERN,
        message = "Use lowercase letters and digits separated by single hyphens",
    )
    val slug: String,

    val description: String? = null,
    val participantType: ParticipantType,
    val participationMode: ParticipationMode,
    val handicapMode: HandicapMode,
)
