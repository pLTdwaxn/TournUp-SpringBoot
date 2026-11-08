package com.abeesmall.tournup.tournament

import com.abeesmall.tournup.ui.UiDataType
import com.abeesmall.tournup.ui.UiField
import com.abeesmall.tournup.ui.UiLayout
import com.abeesmall.tournup.ui.UiLayoutType
import com.abeesmall.tournup.ui.UiManifest
import com.abeesmall.tournup.ui.UiOperation
import com.abeesmall.tournup.ui.UiOption
import com.abeesmall.tournup.ui.UiWidget


fun tournamentListManifest(): UiManifest =
    tournamentReadManifest(
        id = "tournament-list",
        operation = UiOperation.LIST,
        title = "Tournaments",
        layout = UiLayout(
            type = UiLayoutType.TABLE,
            columns = listOf("name", "participantType", "handicapMode"),
        ),
    )

internal fun tournamentReadManifest(
    id: String,
    operation: UiOperation,
    title: String,
    layout: UiLayout,
): UiManifest =
    UiManifest(
        contractVersion = 1,
        id = id,
        application = "tournup-web",
        resource = "tournament",
        operation = operation,
        title = title,
        layout = layout,
        fields = mapOf(
            "name" to displayField("Name"),
            "slug" to displayField("Slug"),
            "description" to displayField("Description"),
            "participantType" to displayField(
                "Participant type",
                UiDataType.ENUM,
                listOf(
                    UiOption("PLAYER", "Player"),
                    UiOption("TEAM", "Team"),
                ),
            ),
            "participationMode" to displayField(
                "Participation mode",
                UiDataType.ENUM,
                listOf(
                    UiOption("WALK_IN", "Walk-in"),
                    UiOption("REGISTRATION", "Registration"),
                ),
            ),
            "handicapMode" to displayField(
                "Handicap mode",
                UiDataType.ENUM,
                listOf(
                    UiOption("NO_HANDICAP", "No handicap"),
                    UiOption("RESET_EACH_SEASON", "Reset each season"),
                    UiOption("CARRY_FORWARD", "Carry forward"),
                ),
            ),
        ),
        commands = emptyList(),
    )

private fun displayField(
    label: String,
    dataType: UiDataType = UiDataType.STRING,
    options: List<UiOption> = emptyList(),
): UiField =
    UiField(
        label = label,
        dataType = dataType,
        widget = UiWidget.DISPLAY,
        editable = false,
        options = options,
    )
