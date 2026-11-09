package com.abeesmall.tournup.dto.tournament

import com.abeesmall.tournup.ui.UiManifest

data class TournamentDetailResponse(
    val manifest: UiManifest,
    val item: TournamentData,
)
