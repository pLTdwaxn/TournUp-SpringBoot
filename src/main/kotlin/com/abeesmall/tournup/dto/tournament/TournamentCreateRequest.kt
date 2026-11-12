package com.abeesmall.tournup.dto.tournament

import com.abeesmall.tournup.validation.WriteOperation

import com.abeesmall.tournup.validation.WriteContract

import com.abeesmall.tournup.validation.ResourceName
import com.abeesmall.tournup.validation.ResourceSlug

import com.abeesmall.tournup.model.tournament.HandicapMode
import com.abeesmall.tournup.model.tournament.ParticipantType
import com.abeesmall.tournup.model.tournament.ParticipationMode

import com.abeesmall.tournup.model.ResourceIdentifiers
import jakarta.validation.constraints.Pattern

data class TournamentCreateRequest(
    @field:ResourceName
    val name: String,

    @field:ResourceSlug
    val slug: String,

    val description: String? = null,
    val participantType: ParticipantType = ParticipantType.PLAYER,
    val participationMode: ParticipationMode = ParticipationMode.WALK_IN,
    val handicapMode: HandicapMode = HandicapMode.NO_HANDICAP,
    @field:Pattern(regexp = ResourceIdentifiers.NANO_ID_PATTERN, message = "NanoID must be four characters from 23456789BCDFGHJKLMNPQRSTVWXYZ")
    val nanoId: String? = null,
)

val tournamentCreateContract = WriteContract(
    TournamentCreateRequest::class, WriteOperation.CREATE,
    initialValues = { TournamentCreateRequest(name = "", slug = "", nanoId = ResourceIdentifiers.nanoId()) },
)
