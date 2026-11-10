package com.abeesmall.tournup.dto.season

import com.abeesmall.tournup.ui.UiManifest

data class SeasonDetailResponse(
    val manifest: UiManifest,
    val item: SeasonData,
)
