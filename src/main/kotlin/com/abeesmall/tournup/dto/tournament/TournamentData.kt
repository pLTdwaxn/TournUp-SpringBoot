package com.abeesmall.tournup.dto.tournament

import com.abeesmall.tournup.dto.audit.AuditedResource
import com.abeesmall.tournup.dto.audit.LastAuditValue
import com.abeesmall.tournup.model.tournament.HandicapMode
import com.abeesmall.tournup.model.tournament.ParticipantType
import com.abeesmall.tournup.model.tournament.ParticipationMode
import com.abeesmall.tournup.ui.resource.Resource
import com.fasterxml.jackson.annotation.JsonInclude
import java.util.UUID

@Resource(key = "tournament", pluralName = "tournaments")
data class TournamentData(
    override val id: UUID,
    val slug: String,
    val name: String,
    val description: String?,
    val participantType: ParticipantType,
    val participationMode: ParticipationMode,
    val handicapMode: HandicapMode,

    @get:JsonInclude(JsonInclude.Include.NON_NULL)
    val lastAudit: LastAuditValue? = null,
) : AuditedResource<TournamentData> {
    override fun withLastAudit(value: LastAuditValue): TournamentData = copy(lastAudit = value)
}
