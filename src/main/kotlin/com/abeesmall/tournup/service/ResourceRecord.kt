package com.abeesmall.tournup.service

/** A data snapshot materialised inside the service transaction, including related references. */
data class ResourceRecord<T>(
    val item: T,
    val included: Map<String, List<Map<String, Any>>> = emptyMap(),
)
