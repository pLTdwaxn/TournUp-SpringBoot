package com.abeesmall.tournup.ui.resource

import com.abeesmall.tournup.dto.ui.UiResourceIdentifier
import com.abeesmall.tournup.dto.ui.UiResourceOperation
import com.abeesmall.tournup.dto.ui.UiResourceRelationship
import com.abeesmall.tournup.manifest.season.seasonCreateFields
import com.abeesmall.tournup.manifest.tournament.tournamentCreateManifest
import com.abeesmall.tournup.model.season.Season
import com.abeesmall.tournup.model.tournament.Tournament
import com.abeesmall.tournup.security.canManageSeasons
import com.abeesmall.tournup.security.canManageTournaments
import com.abeesmall.tournup.ui.UiApplication
import com.abeesmall.tournup.ui.UiCommandMethod
import com.abeesmall.tournup.ui.UiOperation
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration(proxyBeanMethods = false)
class UiResourceConfiguration {
    @Bean
    fun tournamentUiResource(): UiResourceRegistration = UiResourceRegistration(
        modelClass = Tournament::class.java,
        applications = setOf(UiApplication.TOURNAMENT_MANAGER),
        label = "Tournaments",
        displayField = "name",
        fields = tournamentCreateManifest().fields,
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
        ),
        canRead = { it.isAuthenticated },
        canWrite = ::canManageTournaments,
    )

    @Bean
    fun seasonUiResource(): UiResourceRegistration = UiResourceRegistration(
        modelClass = Season::class.java,
        applications = setOf(UiApplication.TOURNAMENT_MANAGER),
        label = "Seasons",
        displayField = "name",
        fields = seasonCreateFields(),
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
        ),
        canRead = { it.isAuthenticated },
        canWrite = ::canManageSeasons,
    )
}
