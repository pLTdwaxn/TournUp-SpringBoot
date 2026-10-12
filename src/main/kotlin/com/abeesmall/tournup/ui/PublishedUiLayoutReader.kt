package com.abeesmall.tournup.ui

import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.UUID

data class PublishedUiLayout(
    val layoutId: UUID,
    val revision: Int,
    val contractVersion: Int,
    val definition: LayoutDefinition,
)

@Service
class PublishedUiLayoutReader(
    private val layouts: UiLayoutRepository,
    private val revisions: UiLayoutRevisionRepository,
) {
    @Transactional(readOnly = true)
    fun find(
        application: String,
        resource: String,
        operation: String,
    ): PublishedUiLayout? {
        val layout = layouts.findByApplicationAndResourceAndOperation(
            application, resource, operation,
        ) ?: return null
        val revisionNumber = layout.publishedRevision ?: return null

        val revision = revisions.findById(
            UiLayoutRevisionId(layout.id, revisionNumber),
        ).orElseThrow {
            IllegalStateException("Published revision is missing")
        }

        return PublishedUiLayout(
            layoutId = layout.id,
            revision = revisionNumber,
            contractVersion = revision.contractVersion,
            definition = revision.definition,
        )
    }
}
