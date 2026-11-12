package com.abeesmall.tournup.dto.season


// The frontend selects the year, ignores nonmatching names, and suggests max S suffix + 1.
data class SeasonNamingContext(
    val tournamentId: String,
    val existingNamesByYear: Map<Int, List<String>>,
)
