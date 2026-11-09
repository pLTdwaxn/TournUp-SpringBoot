package com.abeesmall.tournup.tournament

import com.abeesmall.tournup.ui.UiManifest

data class TournamentUpdateFormResponse(
    val manifest: UiManifest,
    val item: TournamentUpdateRequest,
)
