package com.abeesmall.tournup.dto.season

import com.abeesmall.tournup.validation.WriteContract

import com.abeesmall.tournup.validation.ResourceName
import com.abeesmall.tournup.validation.ResourceSlug
import com.abeesmall.tournup.validation.WriteField
import com.abeesmall.tournup.validation.WriteOperation
import jakarta.validation.constraints.NotNull
import java.util.UUID
import tools.jackson.databind.annotation.JsonDeserialize

data class SeasonWriteRequest(
    @field:ResourceName
    val name: String,

    @field:ResourceSlug
    val slug: String,

    @field:NotNull(message = "From year is required")
    @field:JsonDeserialize(using = SeasonYearDeserializer::class)
    val fromYear: Int?,

    @field:WriteField(writableOn = [WriteOperation.CREATE])
    @field:NotNull(message = "Tournament is required")
    val tournamentId: UUID?,
)

val seasonCreateContract = WriteContract(
    SeasonWriteRequest::class, WriteOperation.CREATE,
    initialValues = { SeasonWriteRequest("", "", null, null) },
)

val seasonUpdateContract = WriteContract(SeasonWriteRequest::class, WriteOperation.UPDATE)
