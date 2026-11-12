package com.abeesmall.tournup.dto.season

import com.abeesmall.tournup.validation.WriteContract

import com.abeesmall.tournup.validation.ResourceName
import com.abeesmall.tournup.validation.ResourceSlug
import com.abeesmall.tournup.validation.WriteField
import com.abeesmall.tournup.validation.WriteOperation
import jakarta.validation.constraints.NotNull
import tools.jackson.databind.annotation.JsonDeserialize

import com.abeesmall.tournup.model.ResourceIdentifiers
import jakarta.validation.constraints.Pattern

data class SeasonWriteRequest(
    @field:ResourceName
    val name: String,

    @field:ResourceSlug
    val slug: String,

    @field:NotNull(message = "From year is required")
    @field:JsonDeserialize(using = SeasonYearDeserializer::class)
    val fromYear: Int?,

    @field:WriteField(writableOn = [WriteOperation.CREATE])
    @field:com.abeesmall.tournup.validation.ResourceLocator
    @field:NotNull(message = "Tournament is required")
    val tournamentId: String?,
    @field:WriteField(writableOn = [WriteOperation.CREATE])
    @field:Pattern(regexp = ResourceIdentifiers.NANO_ID_PATTERN, message = "NanoID must be four characters from 23456789BCDFGHJKLMNPQRSTVWXYZ")
    val nanoId: String? = null,
)

val seasonCreateContract = WriteContract(
    SeasonWriteRequest::class, WriteOperation.CREATE,
    initialValues = { SeasonWriteRequest("", "", null, null, nanoId = ResourceIdentifiers.nanoId()) },
)

val seasonUpdateContract = WriteContract(SeasonWriteRequest::class, WriteOperation.UPDATE)
