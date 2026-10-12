package com.abeesmall.tournup.tournament

import com.abeesmall.tournup.TestcontainersConfiguration
import com.abeesmall.tournup.ui.PublishedUiLayoutReader
import com.abeesmall.tournup.ui.FieldPresentation
import com.abeesmall.tournup.ui.LayoutDefinition
import com.abeesmall.tournup.ui.UiLayout
import com.abeesmall.tournup.ui.UiLayoutRecord
import com.abeesmall.tournup.ui.UiLayoutRepository
import com.abeesmall.tournup.ui.UiLayoutRevisionId
import com.abeesmall.tournup.ui.UiLayoutRevisionRecord
import com.abeesmall.tournup.ui.UiLayoutRevisionRepository
import com.abeesmall.tournup.ui.UiLayoutType
import com.abeesmall.tournup.ui.UiOperation
import com.abeesmall.tournup.ui.UiSection
import com.abeesmall.tournup.ui.UiWidget
import jakarta.persistence.EntityManager
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase
import org.springframework.context.annotation.Import
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNotNull
import kotlin.test.assertNull

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import(
    TestcontainersConfiguration::class,
    TournamentCreateLayoutPublisher::class,
    PublishedUiLayoutReader::class,
)
class TournamentCreateLayoutPublisherTests {
    @Autowired lateinit var layouts: UiLayoutRepository
    @Autowired lateinit var reader: PublishedUiLayoutReader
    @Autowired lateinit var revisions: UiLayoutRevisionRepository
    @Autowired lateinit var publisher: TournamentCreateLayoutPublisher
    @Autowired lateinit var entityManager: EntityManager

    @Test
    fun `publishes a valid revision`() {
        val layout = layouts.saveAndFlush(
            UiLayoutRecord(
                application = "tournup-web",
                resource = "tournament",
                operation = UiOperation.CREATE.wireValue),
        )
        val definition = LayoutDefinition(
            title = "Create tournament",
            layout = UiLayout(
                UiLayoutType.FORM,
                listOf(UiSection("details", "Details", listOf("name", "slug"))),
            ),
            fields = mapOf(
                "name" to FieldPresentation("Name", UiWidget.TEXT),
                "slug" to FieldPresentation("Slug", UiWidget.TEXT),
            ),
        )
        revisions.saveAndFlush(
            UiLayoutRevisionRecord(
                id = UiLayoutRevisionId(layout.id, 1),
                contractVersion = 1,
                definition = definition,
            ),
        )

        publisher.publish(layout.id, 1)
        entityManager.flush()
        entityManager.clear()

        val reloaded = layouts.findById(layout.id).orElseThrow()
        assertEquals(1, reloaded.publishedRevision)

        val published = assertNotNull(
            reader.find("tournup-web", "tournament", UiOperation.CREATE.wireValue),
        )
        assertEquals(layout.id, published.layoutId)
        assertEquals(1, published.revision)
        assertEquals(1, published.contractVersion)
        assertEquals(definition, published.definition)
    }

    @Test
    fun `rejects an invalid revision without publishing it`() {
        val layout = layouts.saveAndFlush(
            UiLayoutRecord(
                application = "tournup-web",
                resource = "tournament",
                operation = UiOperation.CREATE.wireValue,
            ),
        )
        val definition = LayoutDefinition(
            title = "Create tournament",
            layout = UiLayout(
                UiLayoutType.FORM,
                listOf(UiSection("details", "Details", listOf("name"))),
            ),
            fields = mapOf(
                "name" to FieldPresentation("Name", UiWidget.TEXT),
            ),
        )
        revisions.saveAndFlush(
            UiLayoutRevisionRecord(
                id = UiLayoutRevisionId(layout.id, 1),
                contractVersion = 1,
                definition = definition,
            ),
        )

        val error = assertFailsWith<InvalidUiLayoutException> {
            publisher.publish(layout.id, 1)
        }

        assertEquals(setOf("slug"), error.report.omittedRequiredFields)
        assertEquals(null, layout.publishedRevision)
    }

    @Test
    fun `returns no published layout before publication`() {
        layouts.saveAndFlush(
            UiLayoutRecord(
                application = "tournup-web",
                resource = "tournament",
                operation = UiOperation.CREATE.wireValue,
            ),
        )

        assertNull(
            reader.find("tournup-web", "tournament", UiOperation.CREATE.wireValue),
        )
        assertNull(
            reader.find("unknown-app", "tournament", UiOperation.CREATE.wireValue),
        )
    }
}
