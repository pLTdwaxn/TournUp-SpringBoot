package com.abeesmall.tournup.tournament

import com.abeesmall.tournup.ui.UiDataType

data class TournamentFieldDefinition(
    val dataType: UiDataType,
    val required: Boolean,
    val allowedValues: List<String> = emptyList(),
    val defaultValue: String? = null,
)

fun tournamentCreateFieldCatalogue(): Map<String, TournamentFieldDefinition> =
    mapOf(
        "name" to TournamentFieldDefinition(UiDataType.STRING, required = true),
        "slug" to TournamentFieldDefinition(UiDataType.STRING, required = true),
        "description" to TournamentFieldDefinition(UiDataType.STRING, required = false),
        "participantType" to TournamentFieldDefinition(
            dataType = UiDataType.ENUM,
            required = true,
            allowedValues = ParticipantType.entries.map { it.name },
            defaultValue = ParticipantType.PLAYER.name,
        ),
        "participationMode" to TournamentFieldDefinition(
            dataType = UiDataType.ENUM,
            required = true,
            allowedValues = ParticipationMode.entries.map { it.name },
            defaultValue = ParticipationMode.WALK_IN.name,
        ),
        "handicapMode" to TournamentFieldDefinition(
            dataType = UiDataType.ENUM,
            required = true,
            allowedValues = HandicapMode.entries.map { it.name },
            defaultValue = HandicapMode.NO_HANDICAP.name,
        ),
    )
