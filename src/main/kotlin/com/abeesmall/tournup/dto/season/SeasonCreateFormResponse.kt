package com.abeesmall.tournup.dto.season

import com.abeesmall.tournup.ui.UiManifest
import java.util.UUID

data class SeasonCreateFormResponse(
    val manifest: UiManifest,
    val context: SeasonCreateContext,
    val item: SeasonCreateRequest = SeasonCreateRequest(
        name = "",
        slug = "",
        fromYear = null,
    ),
)

/**
 * Input for the frontend's season-name-from-year behaviour.
 * For the selected year, match names of the form "<year> S<n>" and suggest
 * the highest positive suffix plus one, or S1 when there are no matches.
 * Other names are ignored. The suggested name and slug remain editable.
 */
data class SeasonCreateContext(
    val tournamentId: UUID,
    val existingNamesByYear: Map<Int, List<String>>,
)
