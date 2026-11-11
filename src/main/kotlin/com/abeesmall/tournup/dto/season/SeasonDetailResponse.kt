package com.abeesmall.tournup.dto.season

import com.abeesmall.tournup.ui.UiManifest
import java.util.UUID

data class SeasonDetailResponse(
    val manifest: UiManifest,
    val item: SeasonData,
    val context: SeasonDetailContext,
)

data class SeasonDetailContext(
    val tournament: SeasonTournamentContext,
    val collectionHref: String,
)

// These links identify API resources; the web app maps them to its page routes.
data class SeasonTournamentContext(
    val id: UUID,
    val name: String,
    val href: String,
)
