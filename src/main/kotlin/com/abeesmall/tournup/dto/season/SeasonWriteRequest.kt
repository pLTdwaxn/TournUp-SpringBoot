package com.abeesmall.tournup.dto.season

import com.abeesmall.tournup.validation.ResourceName
import com.abeesmall.tournup.validation.ResourceSlug

import jakarta.validation.constraints.NotNull
import tools.jackson.databind.annotation.JsonDeserialize
import java.util.UUID

data class SeasonWriteRequest(
    @field:ResourceName
    val name: String,

    @field:ResourceSlug
    val slug: String,

    @field:NotNull(message = "From year is required")
    @field:JsonDeserialize(using = SeasonYearDeserializer::class)
    val fromYear: Int?,

    @field:NotNull(message = "Tournament is required")
    val tournamentId: UUID?,
)
