package com.abeesmall.tournup.dto.season

import com.abeesmall.tournup.ui.UiManifest

data class SeasonListResponse(
    val manifest: UiManifest,
    val items: List<SeasonData>,
    val pagination: SeasonListPagination,
)

data class SeasonListPagination(
    val page: Int,
    val size: Int,
    val totalElements: Long,
    val totalPages: Int,
)
