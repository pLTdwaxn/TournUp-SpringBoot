package com.abeesmall.tournup.resource.tournament

import com.abeesmall.tournup.dto.tournament.TournamentCreateRequest
import com.abeesmall.tournup.dto.tournament.TournamentData
import com.abeesmall.tournup.dto.tournament.TournamentUpdateRequest
import com.abeesmall.tournup.resource.ResourceAction
import com.abeesmall.tournup.resource.ResourceDefinition
import com.abeesmall.tournup.resource.WriteContract
import com.abeesmall.tournup.resource.WriteOperation
import com.abeesmall.tournup.security.canManageTournaments

val tournamentCreateContract = WriteContract(
    TournamentCreateRequest::class, WriteOperation.CREATE,
    initialValues = { TournamentCreateRequest(name = "", slug = "") },
)
val tournamentUpdateContract = WriteContract(TournamentUpdateRequest::class, WriteOperation.UPDATE)
val tournamentResource = ResourceDefinition(
    resourceClass = TournamentData::class.java,
    create = tournamentCreateContract,
    update = tournamentUpdateContract,
    actions = setOf(ResourceAction.LIST, ResourceAction.READ, ResourceAction.CREATE_FORM,
        ResourceAction.UPDATE_FORM, ResourceAction.CREATE, ResourceAction.UPDATE),
    canRead = { it.isAuthenticated },
    canWrite = ::canManageTournaments,
)
