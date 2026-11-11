package com.abeesmall.tournup.manifest.season

import com.abeesmall.tournup.dto.season.SeasonData
import com.abeesmall.tournup.dto.ui.UiResourceIdentifier
import com.abeesmall.tournup.dto.ui.UiResourceOperation
import com.abeesmall.tournup.dto.ui.UiResourceRelationship
import com.abeesmall.tournup.manifest.season.seasonFieldCatalogue
import com.abeesmall.tournup.security.canManageSeasons
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
import com.abeesmall.tournup.ui.resource.UiResourceRegistration


internal fun seasonResourceDefinition(): UiResourceRegistration {
    val all = listOf("tournamentId", "fromYear", "name", "slug")
    val form = UiLayout(UiLayoutType.FORM, listOf(UiSection("details", "Details", all)))
    val detail = UiLayout(UiLayoutType.DETAIL, listOf(UiSection("details", "Details", all)))
    return UiResourceRegistration(
        resourceClass = SeasonData::class.java,
        audited = true,
        applications = setOf(UiApplication.TOURNAMENT_MANAGER),
        label = "Seasons",
        displayField = "name",
        fields = seasonFieldCatalogue(),
        identifier = UiResourceIdentifier("id", "uuid", slugField = "slug", slugContext = listOf("tournamentId")),
        relationships = listOf(UiResourceRelationship("tournamentId", "tournament", "Tournament", true)),
        operations = listOf(
            UiResourceOperation("list", UiOperation.LIST, UiCommandMethod.GET, "/api/seasons/"),
            UiResourceOperation(
                "list-in-tournament", UiOperation.LIST, UiCommandMethod.GET,
                "/api/tournaments/{tournamentId}/seasons/", listOf("tournamentId"),
            ),
            UiResourceOperation("read", UiOperation.READ, UiCommandMethod.GET, "/api/seasons/{id}/"),
            UiResourceOperation(
                "read-by-slug", UiOperation.READ, UiCommandMethod.GET,
                "/api/tournaments/{tournamentId}/seasons/{slug}/", listOf("tournamentId"),
            ),
            UiResourceOperation(
                "create-form", UiOperation.CREATE, UiCommandMethod.GET,
                "/api/seasons/create/",
            ),
            UiResourceOperation(
                "create", UiOperation.CREATE, UiCommandMethod.POST,
                "/api/seasons/",
            ),
            UiResourceOperation("edit-form", UiOperation.UPDATE, UiCommandMethod.GET, "/api/seasons/{id}/edit/"),
            UiResourceOperation("update", UiOperation.UPDATE, UiCommandMethod.PUT, "/api/seasons/{id}/"),
            UiResourceOperation(
                "naming-context", UiOperation.CREATE, UiCommandMethod.GET,
                "/api/seasons/naming-context/?tournamentId={tournamentId}", listOf("tournamentId"),
            ),
            UiResourceOperation("lookup", UiOperation.LIST, UiCommandMethod.GET, "/api/seasons/lookup/"),
        ),
        canRead = { it.isAuthenticated },
        canWrite = ::canManageSeasons,
        views = mapOf(
            UiOperation.CREATE to UiViewDefinition(
                "season-create", "Create season", form, all,
                behaviours = mapOf(
                    "name" to listOf(
                        UiBehaviour(
                            UiBehaviourType.SEASON_NAME_FROM_YEAR,
                            listOf("tournamentId", "fromYear"), "naming-context"
                        )
                    ),
                    "slug" to listOf(UiBehaviour(UiBehaviourType.SLUGIFY, listOf("name"))),
                ),
                commands = listOf(UiManifestCommand(UiOperation.CREATE, "create", "Create season", "create")),
            ),
            UiOperation.UPDATE to UiViewDefinition(
                "season-update", "Edit season", form, all,
                commands = listOf(UiManifestCommand(UiOperation.UPDATE, "update", "Save changes", "update")),
            ),
            UiOperation.READ to UiViewDefinition(
                "season-detail", "Season", detail, all,
                commands = listOf(UiManifestCommand(UiOperation.UPDATE, "edit", "Edit season", "edit-form")),
            ),
            UiOperation.LIST to UiViewDefinition(
                "season-list", "Seasons", UiLayout(UiLayoutType.TABLE, columns = listOf("name")),
                listOf("fromYear", "name", "slug"),
                commands = listOf(UiManifestCommand(UiOperation.CREATE, "create", "Create season", "create-form", queryParameters = listOf("tournamentId"))),
            ),
        ),
    )
}
