package com.abeesmall.tournup.service.tournament

import com.abeesmall.tournup.dto.tournament.TournamentData
import com.abeesmall.tournup.model.tournament.Tournament


internal fun Tournament.toData(): TournamentData = TournamentData(
    id = id,
    slug = slug,
    name = name,
    description = description,
    participantType = participantType,
    participationMode = participationMode,
    handicapMode = handicapMode,
)

internal fun Tournament.includedReference(): Map<String, List<Map<String, Any>>> =
    mapOf("tournament" to listOf(mapOf("id" to id, "name" to name)))
