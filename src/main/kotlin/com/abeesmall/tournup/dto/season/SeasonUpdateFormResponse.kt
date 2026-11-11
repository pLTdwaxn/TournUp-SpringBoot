package com.abeesmall.tournup.dto.season

import com.abeesmall.tournup.ui.UiManifest

data class SeasonUpdateFormResponse(
    val manifest: UiManifest,
    val item: SeasonUpdateRequest,
    val context: SeasonDetailContext,
    val included: Map<String, List<Map<String, Any>>>,
)
