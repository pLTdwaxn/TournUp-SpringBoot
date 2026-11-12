package com.abeesmall.tournup.resource.season

import com.abeesmall.tournup.dto.season.SeasonData
import com.abeesmall.tournup.dto.season.SeasonWriteRequest
import com.abeesmall.tournup.resource.ResourceAction
import com.abeesmall.tournup.resource.ResourceDefinition
import com.abeesmall.tournup.resource.WriteContract
import com.abeesmall.tournup.resource.WriteOperation
import com.abeesmall.tournup.security.canManageSeasons

val seasonCreateContract = WriteContract(
    SeasonWriteRequest::class, WriteOperation.CREATE,
    initialValues = { SeasonWriteRequest("", "", null, null) },
)
val seasonUpdateContract = WriteContract(SeasonWriteRequest::class, WriteOperation.UPDATE)
val seasonResource = ResourceDefinition(
    resourceClass = SeasonData::class.java,
    create = seasonCreateContract,
    update = seasonUpdateContract,
    actions = setOf(ResourceAction.LIST, ResourceAction.READ, ResourceAction.CREATE_FORM,
        ResourceAction.UPDATE_FORM, ResourceAction.CREATE, ResourceAction.UPDATE),
    canRead = { it.isAuthenticated },
    canWrite = ::canManageSeasons,
)
