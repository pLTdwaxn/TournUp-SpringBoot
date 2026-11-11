package com.abeesmall.tournup.dto.season

import com.abeesmall.tournup.ui.UiManifest

data class SeasonCreateFormResponse(
    val manifest: UiManifest,
    val item: SeasonCreateRequest = SeasonCreateRequest(
        name = "",
        slug = "",
        fromYear = null,
        tournamentId = null,
    ),
    val included: Map<String, List<Map<String, Any>>> = emptyMap(),
)
