package com.abeesmall.tournup.dto.ui

import java.util.UUID

data class LookupItem(val id: UUID, val name: String)
data class LookupResponse(val items: List<LookupItem>, val nextCursor: String?)
