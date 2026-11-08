package com.abeesmall.tournup.tournament

import com.abeesmall.tournup.ui.UiManifest

data class TournamentDetailResponse(
    val manifest: UiManifest,
    val item: TournamentData,
)
