package com.abeesmall.tournup.manifest.tournament

import com.abeesmall.tournup.dto.tournament.tournamentUpdateContract

import com.abeesmall.tournup.dto.tournament.tournamentCreateContract

import com.abeesmall.tournup.dto.tournament.TournamentCreateRequest
import com.abeesmall.tournup.dto.tournament.TournamentData
import com.abeesmall.tournup.dto.tournament.TournamentUpdateRequest
import com.abeesmall.tournup.dto.ui.UiResourceIdentifier
import com.abeesmall.tournup.dto.ui.UiResourceOperation
import com.abeesmall.tournup.model.tournament.HandicapMode
import com.abeesmall.tournup.model.tournament.ParticipantType
import com.abeesmall.tournup.model.tournament.ParticipationMode
import com.abeesmall.tournup.security.canManageTournaments
import com.abeesmall.tournup.ui.UiApplication
import com.abeesmall.tournup.ui.UiBehaviour
import com.abeesmall.tournup.ui.UiBehaviourType
import com.abeesmall.tournup.ui.UiCommandMethod
import com.abeesmall.tournup.ui.UiDataType
import com.abeesmall.tournup.ui.UiFieldPresentation
import com.abeesmall.tournup.ui.UiLayout
import com.abeesmall.tournup.ui.UiLayoutType
import com.abeesmall.tournup.ui.UiManifestCommand
import com.abeesmall.tournup.ui.UiOperation
import com.abeesmall.tournup.ui.UiOption
import com.abeesmall.tournup.ui.UiSection
import com.abeesmall.tournup.ui.UiViewDefinition
import com.abeesmall.tournup.ui.UiWidget
import com.abeesmall.tournup.ui.resource.ResourceAction
import com.abeesmall.tournup.ui.resource.UiResourceRegistration

fun tournamentFieldPresentation(): Map<String, UiFieldPresentation> = mapOf(
    "name" to UiFieldPresentation(
        "Name", UiDataType.STRING,
    ),
    "slug" to UiFieldPresentation(
        "Slug", UiDataType.STRING,
    ),
    "description" to UiFieldPresentation("Description", UiDataType.STRING),
    "participantType" to UiFieldPresentation(
        "Participant type", UiDataType.ENUM,
        options = ParticipantType.entries.map { value ->
            UiOption(value.name, when (value) {
                ParticipantType.PLAYER -> "Player"
                ParticipantType.TEAM -> "Team"
            })
        },
    ),
    "participationMode" to UiFieldPresentation(
        "Participation mode", UiDataType.ENUM,
        options = ParticipationMode.entries.map { value ->
            UiOption(value.name, when (value) {
                ParticipationMode.WALK_IN -> "Walk-in"
                ParticipationMode.REGISTRATION -> "Registration"
            })
        },
    ),
    "handicapMode" to UiFieldPresentation(
        "Handicap mode", UiDataType.ENUM,
        options = HandicapMode.entries.map { value ->
            UiOption(value.name, when (value) {
                HandicapMode.NO_HANDICAP -> "No handicap"
                HandicapMode.RESET_EACH_SEASON -> "Reset each season"
                HandicapMode.CARRY_FORWARD -> "Carry forward"
            })
        },
    ),
)

internal fun tournamentResourceDefinition(): UiResourceRegistration {
    val sections = listOf(
        UiSection("details", "Details", listOf("name", "slug", "description")),
        UiSection("format", "Format", listOf("participantType", "participationMode", "handicapMode")),
    )
    val form = UiLayout(UiLayoutType.FORM, sections)
    val widgets = mapOf("description" to UiWidget.TEXTAREA)
    return UiResourceRegistration(
        resourceClass = TournamentData::class.java,
        create = tournamentCreateContract,
        update = tournamentUpdateContract,
        actions = setOf(ResourceAction.LIST, ResourceAction.READ, ResourceAction.CREATE_FORM,
        ResourceAction.UPDATE_FORM, ResourceAction.CREATE, ResourceAction.UPDATE),
        canRead = { it.isAuthenticated },
        canWrite = ::canManageTournaments,
        audited = true,
        applications = setOf(UiApplication.TOURNAMENT_MANAGER),
        label = "Tournaments",
        displayField = "name",
        presentation = tournamentFieldPresentation(),
        identifier = UiResourceIdentifier("id", "uuid", slugField = "slug"),
        relationships = emptyList(),
        operations = listOf(
            UiResourceOperation("list", UiOperation.LIST, UiCommandMethod.GET, "/api/tournaments/"),
            UiResourceOperation("read", UiOperation.READ, UiCommandMethod.GET, "/api/tournaments/{id}/"),
            UiResourceOperation("read-by-slug", UiOperation.READ, UiCommandMethod.GET, "/api/tournaments/{slug}/"),
            UiResourceOperation("create-form", UiOperation.CREATE, UiCommandMethod.GET, "/api/tournaments/create/"),
            UiResourceOperation("create", UiOperation.CREATE, UiCommandMethod.POST, "/api/tournaments/"),
            UiResourceOperation("edit-form", UiOperation.UPDATE, UiCommandMethod.GET, "/api/tournaments/{id}/edit/"),
            UiResourceOperation("update", UiOperation.UPDATE, UiCommandMethod.PUT, "/api/tournaments/{id}/"),
            UiResourceOperation("lookup", UiOperation.LIST, UiCommandMethod.GET, "/api/tournaments/lookup/"),
        ),
        publishedViews = setOf(UiOperation.LIST),
        views = mapOf(
            UiOperation.CREATE to UiViewDefinition(
                "tournament-create", "Create tournament", form, tournamentFieldPresentation().keys.toList(), widgets,
                behaviours = mapOf("slug" to listOf(UiBehaviour(UiBehaviourType.SLUGIFY, listOf("name")))),
                commands = listOf(UiManifestCommand(UiOperation.CREATE, "create", "Create tournament", "create")),
            ),
            UiOperation.UPDATE to UiViewDefinition(
                "tournament-update", "Edit tournament", form, tournamentFieldPresentation().keys.toList(), widgets,
                commands = listOf(UiManifestCommand(UiOperation.UPDATE, "update", "Save changes", "update")),
            ),
            UiOperation.READ to UiViewDefinition(
                "tournament-detail", "Tournament", UiLayout(UiLayoutType.DETAIL, sections), tournamentFieldPresentation().keys.toList(),
                commands = listOf(UiManifestCommand(UiOperation.UPDATE, "edit", "Edit tournament", "edit-form")),
            ),
            UiOperation.LIST to UiViewDefinition(
                "tournament-list", "Tournaments", UiLayout(UiLayoutType.TABLE, columns = listOf("name")),
                tournamentFieldPresentation().keys.toList(), commands = listOf(UiManifestCommand(UiOperation.CREATE, "create", "Create tournament", "create-form")),
            ),
        ),
    )
}

@org.springframework.context.annotation.Configuration(proxyBeanMethods = false)
class TournamentManifestConfiguration {
    @org.springframework.context.annotation.Bean
    fun tournamentUiResource() = tournamentResourceDefinition()
}
