package com.abeesmall.tournup.manifest.season

import com.abeesmall.tournup.validation.RESOURCE_NAME_MAX_LENGTH
import com.abeesmall.tournup.validation.RESOURCE_SLUG_MAX_LENGTH
import com.abeesmall.tournup.validation.RESOURCE_SLUG_PATTERN
import com.abeesmall.tournup.ui.UiFieldDefinition
import com.abeesmall.tournup.ui.UiDataType
import com.abeesmall.tournup.ui.UiReference


fun seasonFieldCatalogue(): Map<String, UiFieldDefinition> = mapOf(
    "tournamentId" to UiFieldDefinition(
        "Tournament", UiDataType.REFERENCE, required = true,
        reference = UiReference("tournament", "lookup", "read"),
    ),
    "fromYear" to UiFieldDefinition("From year", UiDataType.INTEGER, required = true),
    "name" to UiFieldDefinition(
        "Name", UiDataType.STRING, required = true, maxLength = RESOURCE_NAME_MAX_LENGTH,
    ),
    "slug" to UiFieldDefinition(
        "Slug", UiDataType.STRING, required = true,
        maxLength = RESOURCE_SLUG_MAX_LENGTH, pattern = RESOURCE_SLUG_PATTERN,
    ),
)
