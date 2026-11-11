package com.abeesmall.tournup.manifest.tournament

import com.abeesmall.tournup.dto.tournament.TournamentData
import com.abeesmall.tournup.dto.ui.UiResourceIdentifier
import com.abeesmall.tournup.dto.ui.UiResourceOperation
import com.abeesmall.tournup.manifest.tournament.tournamentFieldCatalogue
import com.abeesmall.tournup.security.canManageTournaments
import com.abeesmall.tournup.ui.UiApplication
import com.abeesmall.tournup.ui.UiBehaviour
import com.abeesmall.tournup.ui.UiBehaviourType
import com.abeesmall.tournup.ui.UiCommandMethod
import com.abeesmall.tournup.ui.UiLayout
import com.abeesmall.tournup.ui.UiLayoutType
import com.abeesmall.tournup.ui.UiManifestCommand
import com.abeesmall.tournup.ui.UiOperation
import com.abeesmall.tournup.ui.UiSection
import com.abeesmall.tournup.ui.UiViewDefinition
import com.abeesmall.tournup.ui.UiWidget
import com.abeesmall.tournup.ui.resource.UiResourceRegistration


internal fun tournamentResourceDefinition(): UiResourceRegistration {
    val fields = tournamentFieldCatalogue()
    val sections = listOf(
        UiSection("details", "Details", listOf("name", "slug", "description")),
        UiSection("format", "Format", listOf("participantType", "participationMode", "handicapMode")),
    )
    val form = UiLayout(UiLayoutType.FORM, sections)
    val widgets = mapOf("description" to UiWidget.TEXTAREA)
    return UiResourceRegistration(
        resourceClass = TournamentData::class.java,
        audited = true,
        applications = setOf(UiApplication.TOURNAMENT_MANAGER),
        label = "Tournaments",
        displayField = "name",
        fields = tournamentFieldCatalogue(),
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
        canRead = { it.isAuthenticated },
        canWrite = ::canManageTournaments,
        views = mapOf(
            UiOperation.CREATE to UiViewDefinition(
                "tournament-create", "Create tournament", form, fields.keys.toList(), widgets,
                behaviours = mapOf("slug" to listOf(UiBehaviour(UiBehaviourType.SLUGIFY, listOf("name")))),
                commands = listOf(UiManifestCommand(UiOperation.CREATE, "create", "Create tournament", "create")),
            ),
            UiOperation.UPDATE to UiViewDefinition(
                "tournament-update", "Edit tournament", form, fields.keys.toList(), widgets,
                commands = listOf(UiManifestCommand(UiOperation.UPDATE, "update", "Save changes", "update")),
            ),
            UiOperation.READ to UiViewDefinition(
                "tournament-detail", "Tournament", UiLayout(UiLayoutType.DETAIL, sections), fields.keys.toList(),
                commands = listOf(UiManifestCommand(UiOperation.UPDATE, "edit", "Edit tournament", "edit-form")),
            ),
            UiOperation.LIST to UiViewDefinition(
                "tournament-list", "Tournaments", UiLayout(UiLayoutType.TABLE, columns = listOf("name")),
                fields.keys.toList(), commands = listOf(UiManifestCommand(UiOperation.CREATE, "create", "Create tournament", "create-form")),
            ),
        ),
    )
}
