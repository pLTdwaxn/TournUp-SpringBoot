package com.abeesmall.tournup.dto.tournament

import com.abeesmall.tournup.model.tournament.HandicapMode
import com.abeesmall.tournup.model.tournament.ParticipantType
import com.abeesmall.tournup.model.tournament.ParticipationMode
import com.abeesmall.tournup.ui.UiManifest
import java.util.UUID

data class TournamentListResponse(
    val manifest: UiManifest,
    val items: List<TournamentData>,
    val pagination: TournamentListPagination,
)

data class TournamentData(
    val id: UUID,
    val slug: String,
    val name: String,
    val description: String?,
    val participantType: ParticipantType,
    val participationMode: ParticipationMode,
    val handicapMode: HandicapMode,
)

data class TournamentListPagination(
    val page: Int,
    val size: Int,
    val totalElements: Long,
    val totalPages: Int,
)
