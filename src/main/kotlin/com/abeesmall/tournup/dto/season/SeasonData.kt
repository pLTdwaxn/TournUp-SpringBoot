package com.abeesmall.tournup.dto.season

import com.abeesmall.tournup.dto.audit.AuditedResource
import com.abeesmall.tournup.dto.audit.LastAuditValue
import com.abeesmall.tournup.resource.Resource
import com.fasterxml.jackson.annotation.JsonInclude
import java.util.UUID

@Resource(key = "season", pluralName = "seasons")
data class SeasonData(
    override val id: UUID,
    val tournamentId: UUID,
    val name: String,
    val slug: String,
    val fromYear: Int,

    @get:JsonInclude(JsonInclude.Include.NON_NULL)
    val lastAudit: LastAuditValue? = null,
) : AuditedResource<SeasonData> {
    override fun withLastAudit(value: LastAuditValue): SeasonData = copy(lastAudit = value)
}
