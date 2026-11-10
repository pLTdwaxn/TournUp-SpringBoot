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
class UiLayoutRepositoryTests {
    @Autowired
    lateinit var repository: UiLayoutRepository

    @Autowired
    lateinit var entityManager: EntityManager

    @Test
    fun `finds a layout by application resource and operation`() {
        val saved = repository.saveAndFlush(
            UiLayoutRecord(
                application = "tournament-manager",
                resource = "tournament",
                operation = "create",
            ),
        )
        entityManager.clear()

        val found = assertNotNull(
            repository.findByApplicationAndResourceAndOperation(
                "tournament-manager",
                "tournament",
                "create",
            ),
        )

        assertEquals(saved.id, found.id)
        assertEquals(null, found.publishedRevision)
    }
}
