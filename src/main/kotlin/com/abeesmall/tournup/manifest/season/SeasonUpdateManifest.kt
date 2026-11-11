package com.abeesmall.tournup.manifest.season

import com.abeesmall.tournup.ui.UiCommand
import com.abeesmall.tournup.ui.UiCommandMethod
import com.abeesmall.tournup.ui.UiManifest
import com.abeesmall.tournup.ui.UiOperation
import com.abeesmall.tournup.ui.UiApplication
import java.util.UUID

fun seasonUpdateManifest(id: UUID): UiManifest {
    val base = seasonCreateManifest(UiApplication.TOURNAMENT_MANAGER)
    return base.copy(
        id = "season-update",
        operation = UiOperation.UPDATE,
        title = "Edit season",
        fields = base.fields.mapValues { (fieldId, field) ->
            field.copy(editable = fieldId != "tournamentId", behaviours = emptyList())
        },
        commands = listOf(
            UiCommand(
                id = "update",
                label = "Save changes",
                method = UiCommandMethod.PUT,
                href = "/api/seasons/$id/",
            ),
        ),
    )
}
