package com.abeesmall.tournup.tournament

import com.abeesmall.tournup.ui.UiCommand
import com.abeesmall.tournup.ui.UiCommandMethod
import com.abeesmall.tournup.ui.UiManifest
import com.abeesmall.tournup.ui.UiOperation
import java.util.UUID

fun tournamentUpdateManifest(id: UUID): UiManifest {
    val base = tournamentCreateManifest()

    return base.copy(
        id = "tournament-update",
        operation = UiOperation.UPDATE,
        title = "Edit tournament",
        fields = base.fields.mapValues { (fieldId, field) ->
            if (fieldId == "slug") {
                field.copy(behaviours = emptyList())
            } else {
                field
            }
        },
        commands = listOf(
            UiCommand(
                id = "update",
                label = "Save changes",
                method = UiCommandMethod.PUT,
                href = "/api/tournaments/$id/",
            ),
        ),
    )
}
