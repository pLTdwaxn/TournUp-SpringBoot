package com.abeesmall.tournup.service.tournament

import com.abeesmall.tournup.manifest.tournament.tournamentFieldCatalogue
import com.abeesmall.tournup.ui.LayoutValidationReport
import com.abeesmall.tournup.ui.UiLayoutRepository
import com.abeesmall.tournup.ui.UiLayoutRevisionId
import com.abeesmall.tournup.ui.UiLayoutRevisionRepository
import com.abeesmall.tournup.ui.UiLayoutType
import com.abeesmall.tournup.ui.UiOperation
import com.abeesmall.tournup.ui.validateLayoutDefinition
import java.util.UUID
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional


class InvalidUiLayoutException(
    val report: LayoutValidationReport,
) : RuntimeException("UI layout failed validation")

@Service
class TournamentCreateLayoutPublisher(
    private val layouts: UiLayoutRepository,
    private val revisions: UiLayoutRevisionRepository,
) {
    @Transactional
    fun publish(layoutId: UUID, revisionNumber: Int) {
        val layout = layouts.findById(layoutId).orElseThrow {
            IllegalArgumentException("Unknown layout: $layoutId")
        }

        require(
            layout.resource == "tournament" &&
                    layout.operation == UiOperation.CREATE.wireValue
        ) {
            "Not a tournament creation layout"
        }

        val revision = revisions.findById(
            UiLayoutRevisionId(layoutId, revisionNumber),
        ).orElseThrow {
            IllegalArgumentException("Unknown layout revision: $revisionNumber")
        }

        require(revision.definition.layout.type == UiLayoutType.FORM) {
            "Tournament creation requires a form layout"
        }

        val catalogue = tournamentFieldCatalogue()
        val report = validateLayoutDefinition(
            revision.definition,
            catalogue.mapValues { it.value.dataType },
            catalogue.filterValues { it.required && it.defaultValue == null }.keys,
        )
        if (!report.isValid) throw InvalidUiLayoutException(report)

        layout.publishedRevision = revisionNumber
    }
}
