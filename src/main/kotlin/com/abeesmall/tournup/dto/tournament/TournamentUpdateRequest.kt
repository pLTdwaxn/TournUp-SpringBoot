package com.abeesmall.tournup.dto.tournament

import com.abeesmall.tournup.validation.WriteOperation

import com.abeesmall.tournup.validation.WriteContract

import com.abeesmall.tournup.validation.ResourceName
import com.abeesmall.tournup.validation.ResourceSlug

import com.abeesmall.tournup.model.tournament.HandicapMode
import com.abeesmall.tournup.model.tournament.ParticipantType
import com.abeesmall.tournup.model.tournament.ParticipationMode

data class TournamentUpdateRequest(
    @field:ResourceName
    val name: String,

    @field:ResourceSlug
    val slug: String,

    val description: String? = null,
    val participantType: ParticipantType,
    val participationMode: ParticipationMode,
    val handicapMode: HandicapMode,
)

val tournamentUpdateContract = WriteContract(TournamentUpdateRequest::class, WriteOperation.UPDATE)
