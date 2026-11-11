package com.abeesmall.tournup.dto.tournament

import com.abeesmall.tournup.ui.resource.UiResource
import com.abeesmall.tournup.ui.UiFieldPolicy
import com.abeesmall.tournup.ui.UiOperation

import com.abeesmall.tournup.dto.audit.LastAuditValue
import com.abeesmall.tournup.model.tournament.HandicapMode
import com.abeesmall.tournup.model.tournament.ParticipantType
import com.abeesmall.tournup.model.tournament.ParticipationMode

import com.fasterxml.jackson.annotation.JsonInclude
import com.abeesmall.tournup.dto.audit.AuditedResource
import java.util.UUID

@UiResource(key = "tournament", pluralName = "tournaments")
data class TournamentData(
    override val id: UUID,
    @field:UiFieldPolicy(writableOn = [UiOperation.CREATE, UiOperation.UPDATE])
    val slug: String,
    @field:UiFieldPolicy(writableOn = [UiOperation.CREATE, UiOperation.UPDATE])
    val name: String,
    @field:UiFieldPolicy(writableOn = [UiOperation.CREATE, UiOperation.UPDATE])
    val description: String?,
    @field:UiFieldPolicy(writableOn = [UiOperation.CREATE, UiOperation.UPDATE])
    val participantType: ParticipantType,
    @field:UiFieldPolicy(writableOn = [UiOperation.CREATE, UiOperation.UPDATE])
    val participationMode: ParticipationMode,
    @field:UiFieldPolicy(writableOn = [UiOperation.CREATE, UiOperation.UPDATE])
    val handicapMode: HandicapMode,

    @get:JsonInclude(JsonInclude.Include.NON_NULL)
    val lastAudit: LastAuditValue? = null,
) : AuditedResource<TournamentData> {
    override fun withLastAudit(value: LastAuditValue): TournamentData = copy(lastAudit = value)
}
