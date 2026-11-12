package com.abeesmall.tournup.manifest.tournament

import com.abeesmall.tournup.model.tournament.HandicapMode
import com.abeesmall.tournup.model.tournament.ParticipantType
import com.abeesmall.tournup.model.tournament.ParticipationMode
import com.abeesmall.tournup.ui.UiDataType
import com.abeesmall.tournup.ui.UiFieldPresentation
import com.abeesmall.tournup.ui.UiOption

fun tournamentFieldPresentation(): Map<String, UiFieldPresentation> = mapOf(
    "name" to UiFieldPresentation(
        "Name", UiDataType.STRING,
    ),
    "slug" to UiFieldPresentation(
        "Slug", UiDataType.STRING,
    ),
    "description" to UiFieldPresentation("Description", UiDataType.STRING),
    "participantType" to UiFieldPresentation(
        "Participant type", UiDataType.ENUM,
        options = ParticipantType.entries.map { value ->
            UiOption(value.name, when (value) {
                ParticipantType.PLAYER -> "Player"
                ParticipantType.TEAM -> "Team"
            })
        },
    ),
    "participationMode" to UiFieldPresentation(
        "Participation mode", UiDataType.ENUM,
        options = ParticipationMode.entries.map { value ->
            UiOption(value.name, when (value) {
                ParticipationMode.WALK_IN -> "Walk-in"
                ParticipationMode.REGISTRATION -> "Registration"
            })
        },
    ),
    "handicapMode" to UiFieldPresentation(
        "Handicap mode", UiDataType.ENUM,
        options = HandicapMode.entries.map { value ->
            UiOption(value.name, when (value) {
                HandicapMode.NO_HANDICAP -> "No handicap"
                HandicapMode.RESET_EACH_SEASON -> "Reset each season"
                HandicapMode.CARRY_FORWARD -> "Carry forward"
            })
        },
    ),
)
