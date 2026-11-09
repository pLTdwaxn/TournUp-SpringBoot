package com.abeesmall.tournup.dto.tournament

import com.abeesmall.tournup.dto.audit.LastAuditValue
import com.abeesmall.tournup.model.tournament.HandicapMode
import com.abeesmall.tournup.model.tournament.ParticipantType
import com.abeesmall.tournup.model.tournament.ParticipationMode
import com.abeesmall.tournup.ui.UiManifest

import com.fasterxml.jackson.annotation.JsonInclude
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

    @get:JsonInclude(JsonInclude.Include.NON_NULL)
    val lastAudit: LastAuditValue? = null,
)

data class TournamentListPagination(
    val page: Int,
    val size: Int,
    val totalElements: Long,
    val totalPages: Int,
)
