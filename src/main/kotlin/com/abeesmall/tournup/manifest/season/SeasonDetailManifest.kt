package com.abeesmall.tournup.manifest.season

import com.abeesmall.tournup.ui.UiDataType
import com.abeesmall.tournup.ui.UiField
import com.abeesmall.tournup.ui.UiLayout
import com.abeesmall.tournup.ui.UiLayoutType
import com.abeesmall.tournup.ui.UiManifest
import com.abeesmall.tournup.ui.UiOperation
import com.abeesmall.tournup.ui.UiSection
import com.abeesmall.tournup.ui.UiWidget

fun seasonDetailManifest(title: String): UiManifest =
    seasonReadManifest(
        id = "season-detail",
        operation = UiOperation.READ,
        title = title,
        layout = UiLayout(
            type = UiLayoutType.DETAIL,
            sections = listOf(
                UiSection("details", "Details", listOf("fromYear", "name", "slug")),
            ),
        ),
    )

internal fun seasonReadManifest(
    id: String,
    operation: UiOperation,
    title: String,
    layout: UiLayout,
): UiManifest =
    UiManifest(
        contractVersion = 1,
        id = id,
        application = "tournup-web",
        resource = "season",
        operation = operation,
        title = title,
        layout = layout,
        fields = mapOf(
            "fromYear" to UiField(
                label = "From year",
                dataType = UiDataType.INTEGER,
                widget = UiWidget.DISPLAY,
                editable = false,
            ),
            "name" to UiField(
                label = "Name",
                dataType = UiDataType.STRING,
                widget = UiWidget.DISPLAY,
                editable = false,
            ),
            "slug" to UiField(
                label = "Slug",
                dataType = UiDataType.STRING,
                widget = UiWidget.DISPLAY,
                editable = false,
            ),
        ),
        commands = emptyList(),
    )
