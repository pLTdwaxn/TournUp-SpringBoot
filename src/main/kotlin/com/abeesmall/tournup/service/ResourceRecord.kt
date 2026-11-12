package com.abeesmall.tournup.service

data class ResourceRecord<T>(
    val item: T,
    val included: Map<String, List<Map<String, Any>>> = emptyMap(),
)
