package com.abeesmall.tournup.dto.ui


data class LookupItem(val id: String, val name: String)
data class LookupResponse(val items: List<LookupItem>, val nextCursor: String?)
