package com.abeesmall.tournup.dto.season

import java.util.UUID

// The frontend selects the year, ignores nonmatching names, and suggests max S suffix + 1.
data class SeasonNamingContext(
    val tournamentId: UUID,
    val existingNamesByYear: Map<Int, List<String>>,
)
