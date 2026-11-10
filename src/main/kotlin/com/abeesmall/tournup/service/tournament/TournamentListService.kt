package com.abeesmall.tournup.service.tournament

import com.abeesmall.tournup.dto.tournament.TournamentData
import com.abeesmall.tournup.dto.tournament.TournamentListPagination
import com.abeesmall.tournup.dto.tournament.TournamentListResponse
import com.abeesmall.tournup.manifest.tournament.tournamentListManifest
import com.abeesmall.tournup.repository.tournament.TournamentRepository
import com.abeesmall.tournup.ui.PublishedUiLayoutReader
import com.abeesmall.tournup.ui.UiApplication
import com.abeesmall.tournup.ui.UiLayoutType
import com.abeesmall.tournup.ui.UiManifest
import com.abeesmall.tournup.ui.validateLayoutDefinition
import org.slf4j.LoggerFactory
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class TournamentListService(
    private val tournaments: TournamentRepository,
    private val layouts: PublishedUiLayoutReader,
) {
    private val logger = LoggerFactory.getLogger(TournamentListService::class.java)

    @Transactional(readOnly = true)
    fun list(
        pageable: Pageable,
        application: UiApplication,
        canCreate: Boolean = false,
    ): TournamentListResponse {
        val result = tournaments.findAll(pageable)

        return TournamentListResponse(
            manifest = listManifest(canCreate, application),
            items = result.content.map { tournament ->
                TournamentData(
                    id = tournament.id,
                    slug = tournament.slug,
                    name = tournament.name,
                    description = tournament.description,
                    participantType = tournament.participantType,
                    participationMode = tournament.participationMode,
                    handicapMode = tournament.handicapMode,
                )
            },
            pagination = TournamentListPagination(
                page = result.number,
                size = result.size,
                totalElements = result.totalElements,
                totalPages = result.totalPages,
            ),
        )
    }

    private fun listManifest(canCreate: Boolean, application: UiApplication): UiManifest {
        val base = tournamentListManifest(application, canCreate)
        val published = layouts.find(base.application, base.resource, base.operation.wireValue)
            ?: return base
        val definition = published.definition
        val validation = validateLayoutDefinition(
            definition,
            base.fields.mapValues { it.value.dataType },
            requiredWithoutDefaults = emptySet(),
        )
        if (published.contractVersion != base.contractVersion ||
            definition.layout.type != UiLayoutType.TABLE || !validation.isValid
        ) {
            logger.warn(
                "Ignoring invalid tournament list layout {} revision {} for application {}",
                published.layoutId, published.revision, application.id,
            )
            return base
        }

        return base.copy(
            title = definition.title,
            layout = definition.layout,
            fields = base.fields.mapValues { (id, field) ->
                definition.fields[id]?.let { presentation ->
                    field.copy(label = presentation.label, widget = presentation.widget)
                } ?: field
            },
        )
    }
}
