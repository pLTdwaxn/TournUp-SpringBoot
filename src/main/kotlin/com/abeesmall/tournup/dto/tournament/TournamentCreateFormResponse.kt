package com.abeesmall.tournup.dto.tournament

import com.abeesmall.tournup.ui.UiManifest

data class TournamentCreateFormResponse(
    val manifest: UiManifest,
    val item: TournamentCreateRequest = TournamentCreateRequest(
        name = "",
        slug = "",
    ),
)
