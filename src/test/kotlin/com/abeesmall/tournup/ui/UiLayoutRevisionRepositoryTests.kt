package com.abeesmall.tournup.ui

import com.abeesmall.tournup.TestcontainersConfiguration
import jakarta.persistence.EntityManager
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase
import org.springframework.context.annotation.Import
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import(TestcontainersConfiguration::class)
class UiLayoutRevisionRepositoryTests {
    @Autowired
    lateinit var layouts: UiLayoutRepository

    @Autowired
    lateinit var revisions: UiLayoutRevisionRepository

    @Autowired
    lateinit var entityManager: EntityManager

    @Test
    fun `round trips a layout definition through jsonb`() {
        val layout = layouts.saveAndFlush(
            UiLayoutRecord(
                application = "tournament-manager",
                resource = "tournament",
                operation = "create",
            ),
        )
        val definition = LayoutDefinition(
            title = "Create tournament",
            layout = UiLayout(
                type = UiLayoutType.FORM,
                sections = listOf(
                    UiSection("details", "Details", listOf("name", "slug")),
                ),
            ),
            fields = mapOf(
                "name" to FieldPresentation("Name", UiWidget.TEXT),
                "slug" to FieldPresentation("Slug", UiWidget.TEXT),
            ),
        )
        val id = UiLayoutRevisionId(layout.id, 1)

        revisions.saveAndFlush(
            UiLayoutRevisionRecord(
                id = id,
                contractVersion = 1,
                definition = definition,
            )
        )
        entityManager.clear()

        val found = assertNotNull(revisions.findById(id).orElse(null))
        assertEquals(1, found.contractVersion)
        assertEquals(definition, found.definition)
    }
}
