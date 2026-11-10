package com.abeesmall.tournup.manifest.season

import com.abeesmall.tournup.ui.UiCommand
import com.abeesmall.tournup.ui.UiCommandMethod
import com.abeesmall.tournup.ui.UiLayout
import com.abeesmall.tournup.ui.UiLayoutType
import com.abeesmall.tournup.ui.UiManifest
import com.abeesmall.tournup.ui.UiOperation
import java.util.UUID

fun seasonListManifest(tournamentId: UUID, canCreate: Boolean = false): UiManifest =
    seasonReadManifest(
        id = "season-list",
        operation = UiOperation.LIST,
        title = "Seasons",
        layout = UiLayout(
            type = UiLayoutType.TABLE,
            columns = listOf("name", "fromYear", "slug"),
        ),
    ).copy(
        commands = if (canCreate) {
            listOf(
                UiCommand(
                    id = "create",
                    label = "Create season",
                    method = UiCommandMethod.GET,
                    href = "/api/tournaments/$tournamentId/seasons/create/",
                ),
            )
        } else {
            emptyList()
        },
    )
