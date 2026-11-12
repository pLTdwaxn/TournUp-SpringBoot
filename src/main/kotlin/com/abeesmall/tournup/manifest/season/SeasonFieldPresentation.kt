package com.abeesmall.tournup.manifest.season

import com.abeesmall.tournup.ui.UiDataType
import com.abeesmall.tournup.ui.UiFieldPresentation
import com.abeesmall.tournup.ui.UiReference

fun seasonFieldPresentation(): Map<String, UiFieldPresentation> = mapOf(
    "tournamentId" to UiFieldPresentation(
        "Tournament", UiDataType.REFERENCE,
        reference = UiReference("tournament", "lookup", "read"),
    ),
    "fromYear" to UiFieldPresentation("From year", UiDataType.INTEGER),
    "name" to UiFieldPresentation(
        "Name", UiDataType.STRING,
    ),
    "slug" to UiFieldPresentation(
        "Slug", UiDataType.STRING,
    ),
)
