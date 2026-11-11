package com.abeesmall.tournup.manifest.season

import com.abeesmall.tournup.dto.season.SEASON_NAME_MAX_LENGTH
import com.abeesmall.tournup.dto.season.SEASON_SLUG_MAX_LENGTH
import com.abeesmall.tournup.dto.season.SEASON_SLUG_PATTERN
import com.abeesmall.tournup.ui.UiBehaviour
import com.abeesmall.tournup.ui.UiApplication
import com.abeesmall.tournup.ui.UiBehaviourType
import com.abeesmall.tournup.ui.UiCommand
import com.abeesmall.tournup.ui.UiCommandMethod
import com.abeesmall.tournup.ui.UiDataType
import com.abeesmall.tournup.ui.UiField
import com.abeesmall.tournup.ui.UiLayout
import com.abeesmall.tournup.ui.UiLayoutType
import com.abeesmall.tournup.ui.UiManifest
import com.abeesmall.tournup.ui.UiOperation
import com.abeesmall.tournup.ui.UiSection
import com.abeesmall.tournup.ui.UiWidget
import com.abeesmall.tournup.ui.UiReference

fun seasonCreateManifest(application: UiApplication): UiManifest =
    UiManifest(
        contractVersion = 1,
        id = "season-create",
        application = application.id,
        resource = "season",
        operation = UiOperation.CREATE,
        title = "Create season",
        layout = UiLayout(
            type = UiLayoutType.FORM,
            sections = listOf(
                UiSection(
                    id = "details",
                    title = "Details",
                    fields = listOf("tournamentId", "fromYear", "name", "slug"),
                ),
            ),
        ),
        fields = seasonCreateFields(),
        commands = listOf(
            UiCommand(
                id = "create",
                label = "Create season",
                method = UiCommandMethod.POST,
                href = "/api/seasons/",
            ),
        ),
    )

internal fun seasonCreateFields(): Map<String, UiField> =
    mapOf(
        "tournamentId" to UiField(
            label = "Tournament",
            dataType = UiDataType.REFERENCE,
            widget = UiWidget.RECORD_SELECT,
            required = true,
            editable = true,
            reference = UiReference("tournament", "list", "read"),
        ),
        "fromYear" to UiField(
            label = "From year",
            dataType = UiDataType.INTEGER,
            widget = UiWidget.NUMBER,
            required = true,
            editable = true,
        ),
        "name" to UiField(
            label = "Name",
            dataType = UiDataType.STRING,
            widget = UiWidget.TEXT,
            required = true,
            editable = true,
            behaviours = listOf(
                UiBehaviour(
                    type = UiBehaviourType.SEASON_NAME_FROM_YEAR,
                    dependsOn = listOf("tournamentId", "fromYear"),
                    contextOperation = "naming-context",
                ),
            ),
            maxLength = SEASON_NAME_MAX_LENGTH,
        ),
        "slug" to UiField(
            label = "Slug",
            dataType = UiDataType.STRING,
            widget = UiWidget.TEXT,
            required = true,
            editable = true,
            behaviours = listOf(
                UiBehaviour(
                    type = UiBehaviourType.SLUGIFY,
                    dependsOn = listOf("name"),
                ),
            ),
            maxLength = SEASON_SLUG_MAX_LENGTH,
            pattern = SEASON_SLUG_PATTERN,
        ),
    )
