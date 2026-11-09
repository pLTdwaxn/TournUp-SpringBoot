package com.abeesmall.tournup.manifest.tournament

import com.abeesmall.tournup.dto.tournament.TOURNAMENT_NAME_MAX_LENGTH
import com.abeesmall.tournup.dto.tournament.TOURNAMENT_SLUG_MAX_LENGTH
import com.abeesmall.tournup.dto.tournament.TOURNAMENT_SLUG_PATTERN
import com.abeesmall.tournup.ui.UiBehaviour
import com.abeesmall.tournup.ui.UiBehaviourType
import com.abeesmall.tournup.ui.UiCommand
import com.abeesmall.tournup.ui.UiCommandMethod
import com.abeesmall.tournup.ui.UiDataType
import com.abeesmall.tournup.ui.UiLayout
import com.abeesmall.tournup.ui.UiLayoutType
import com.abeesmall.tournup.ui.UiManifest
import com.abeesmall.tournup.ui.UiOperation
import com.abeesmall.tournup.ui.UiSection
import com.abeesmall.tournup.ui.UiWidget

fun tournamentCreateManifest(): UiManifest {
    val base = tournamentManifest(
        id = "tournament-create",
        operation = UiOperation.CREATE,
        title = "Create tournament",
        layout = UiLayout(
            type = UiLayoutType.FORM,
            sections = listOf(
                UiSection(
                    id = "details",
                    title = "Details",
                    fields = listOf("name", "slug", "description"),
                ),
                UiSection(
                    id = "format",
                    title = "Format",
                    fields = listOf(
                        "participantType",
                        "participationMode",
                        "handicapMode",
                    ),
                ),
            ),
        ),
    )

    return base.copy(
        fields = tournamentCreateFieldCatalogue().mapValues { (fieldId, definition) ->
            base.fields.getValue(fieldId).copy(
                dataType = definition.dataType,
                widget = when (definition.dataType) {
                    UiDataType.STRING ->
                        if (fieldId == "description") UiWidget.TEXTAREA
                        else UiWidget.TEXT

                    UiDataType.ENUM -> UiWidget.SELECT
                },
                required = definition.required,
                editable = true,
                behaviours = if (fieldId == "slug") {
                    listOf(UiBehaviour(UiBehaviourType.SLUGIFY, "name"))
                } else {
                    emptyList()
                },
                maxLength = when (fieldId) {
                    "name" -> TOURNAMENT_NAME_MAX_LENGTH
                    "slug" -> TOURNAMENT_SLUG_MAX_LENGTH
                    else -> null
                },
                pattern = if (fieldId == "slug") TOURNAMENT_SLUG_PATTERN else null,
            )
        },
        commands = listOf(
            UiCommand(
                id = "create",
                label = "Create tournament",
                method = UiCommandMethod.POST,
                href = "/api/tournaments/",
            ),
        ),
    )
}
