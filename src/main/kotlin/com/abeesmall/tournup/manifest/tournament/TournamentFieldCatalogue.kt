package com.abeesmall.tournup.manifest.tournament

import com.abeesmall.tournup.validation.RESOURCE_NAME_MAX_LENGTH
import com.abeesmall.tournup.validation.RESOURCE_SLUG_MAX_LENGTH
import com.abeesmall.tournup.validation.RESOURCE_SLUG_PATTERN
import com.abeesmall.tournup.model.tournament.HandicapMode
import com.abeesmall.tournup.model.tournament.ParticipantType
import com.abeesmall.tournup.model.tournament.ParticipationMode
import com.abeesmall.tournup.ui.UiDataType
import com.abeesmall.tournup.ui.UiFieldDefinition
import com.abeesmall.tournup.ui.UiOption

fun tournamentFieldCatalogue(): Map<String, UiFieldDefinition> = mapOf(
    "name" to UiFieldDefinition(
        "Name", UiDataType.STRING, required = true, maxLength = RESOURCE_NAME_MAX_LENGTH,
    ),
    "slug" to UiFieldDefinition(
        "Slug", UiDataType.STRING, required = true,
        maxLength = RESOURCE_SLUG_MAX_LENGTH, pattern = RESOURCE_SLUG_PATTERN,
    ),
    "description" to UiFieldDefinition("Description", UiDataType.STRING, required = false),
    "participantType" to UiFieldDefinition(
        "Participant type", UiDataType.ENUM, required = true,
        options = ParticipantType.entries.map { value ->
            UiOption(value.name, when (value) {
                ParticipantType.PLAYER -> "Player"
                ParticipantType.TEAM -> "Team"
            })
        },
        defaultValue = ParticipantType.PLAYER.name,
    ),
    "participationMode" to UiFieldDefinition(
        "Participation mode", UiDataType.ENUM, required = true,
        options = ParticipationMode.entries.map { value ->
            UiOption(value.name, when (value) {
                ParticipationMode.WALK_IN -> "Walk-in"
                ParticipationMode.REGISTRATION -> "Registration"
            })
        },
        defaultValue = ParticipationMode.WALK_IN.name,
    ),
    "handicapMode" to UiFieldDefinition(
        "Handicap mode", UiDataType.ENUM, required = true,
        options = HandicapMode.entries.map { value ->
            UiOption(value.name, when (value) {
                HandicapMode.NO_HANDICAP -> "No handicap"
                HandicapMode.RESET_EACH_SEASON -> "Reset each season"
                HandicapMode.CARRY_FORWARD -> "Carry forward"
            })
        },
        defaultValue = HandicapMode.NO_HANDICAP.name,
    ),
)
