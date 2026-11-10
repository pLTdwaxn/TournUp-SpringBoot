package com.abeesmall.tournup.dto.season

import com.abeesmall.tournup.dto.audit.LastAuditValue
import com.fasterxml.jackson.annotation.JsonInclude
import java.util.UUID

data class SeasonData(
    val id: UUID,
    val tournamentId: UUID,
    val name: String,
    val slug: String,
    val fromYear: Int,

    @get:JsonInclude(JsonInclude.Include.NON_NULL)
    val lastAudit: LastAuditValue? = null,
)
