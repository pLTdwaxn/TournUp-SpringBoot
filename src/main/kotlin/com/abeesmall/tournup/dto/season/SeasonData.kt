package com.abeesmall.tournup.dto.season

import com.abeesmall.tournup.ui.resource.UiResource
import com.abeesmall.tournup.ui.UiFieldPolicy
import com.abeesmall.tournup.ui.UiOperation

import com.abeesmall.tournup.dto.audit.LastAuditValue
import com.fasterxml.jackson.annotation.JsonInclude
import com.abeesmall.tournup.dto.audit.AuditedResource
import java.util.UUID

@UiResource(key = "season", pluralName = "seasons")
data class SeasonData(
    override val id: UUID,
    @field:UiFieldPolicy(writableOn = [UiOperation.CREATE])
    val tournamentId: UUID,
    @field:UiFieldPolicy(writableOn = [UiOperation.CREATE, UiOperation.UPDATE])
    val name: String,
    @field:UiFieldPolicy(writableOn = [UiOperation.CREATE, UiOperation.UPDATE])
    val slug: String,
    @field:UiFieldPolicy(writableOn = [UiOperation.CREATE, UiOperation.UPDATE])
    val fromYear: Int,

    @get:JsonInclude(JsonInclude.Include.NON_NULL)
    val lastAudit: LastAuditValue? = null,
) : AuditedResource<SeasonData> {
    override fun withLastAudit(value: LastAuditValue): SeasonData = copy(lastAudit = value)
}
