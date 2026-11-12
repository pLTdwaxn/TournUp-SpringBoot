package com.abeesmall.tournup.dto.season

import com.abeesmall.tournup.dto.audit.AuditedResource
import com.abeesmall.tournup.dto.audit.LastAuditValue
import com.abeesmall.tournup.ui.resource.Resource
import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.annotation.JsonIgnore
import com.abeesmall.tournup.model.ResourceIdentifiers
import java.util.UUID

@Resource(key = "season", pluralName = "seasons")
data class SeasonData(
    @get:JsonIgnore
    override val internalId: UUID,
    val tournamentId: String,
    val name: String,
    val slug: String,
    val fromYear: Int,

    @get:JsonInclude(JsonInclude.Include.NON_NULL)
    val lastAudit: LastAuditValue? = null,
    val nanoId: String,
) : AuditedResource<SeasonData> {
    override val id: String get() = ResourceIdentifiers.locator(slug, nanoId)
    override fun withLastAudit(value: LastAuditValue): SeasonData = copy(lastAudit = value)
}
