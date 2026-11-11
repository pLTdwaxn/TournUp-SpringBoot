package com.abeesmall.tournup.dto.ui

import com.abeesmall.tournup.ui.UiManifest

data class UiRecordResponse<T>(
    val manifest: UiManifest,
    val item: T,
    val included: Map<String, List<Map<String, Any>>> = emptyMap(),
)

data class UiCollectionResponse<T>(
    val manifest: UiManifest,
    val items: List<T>,
    val pagination: UiPagination,
    val included: Map<String, List<Map<String, Any>>> = emptyMap(),
)

data class UiPagination(
    val page: Int,
    val size: Int,
    val totalElements: Long,
    val totalPages: Int,
)
