package com.abeesmall.tournup.dto.season

import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotNull
import jakarta.validation.constraints.Pattern
import jakarta.validation.constraints.Size
import tools.jackson.databind.annotation.JsonDeserialize

data class SeasonUpdateRequest(
    @field:NotBlank(message = "Name is required")
    @field:Size(
        max = SEASON_NAME_MAX_LENGTH,
        message = "Name must be at most {max} characters",
    )
    val name: String,

    @field:NotBlank(message = "Slug is required")
    @field:Size(
        max = SEASON_SLUG_MAX_LENGTH,
        message = "Slug must be at most {max} characters",
    )
    @field:Pattern(
        regexp = SEASON_SLUG_PATTERN,
        message = "Use lowercase letters and digits separated by single hyphens",
    )
    val slug: String,

    @field:NotNull(message = "From year is required")
    @field:JsonDeserialize(using = SeasonYearDeserializer::class)
    val fromYear: Int?,
)
