package com.abeesmall.tournup.tournament

import com.abeesmall.tournup.TestcontainersConfiguration
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase
import org.springframework.context.annotation.Import
import jakarta.persistence.EntityManager
import java.time.Instant
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNotSame
import kotlin.test.assertTrue

@DataJpaTest
@AutoConfigureTestDatabase(
    replace = AutoConfigureTestDatabase.Replace.NONE,
)
@Import(TestcontainersConfiguration::class)
class TournamentRepositoryTests {

    @Autowired
    lateinit var repository: TournamentRepository

    @Autowired
    lateinit var entityManager: EntityManager

    @Test
    fun `saves and finds a tournament by slug`() {
        val tournament = Tournament(
            slug = "spring-championship",
            name = "Spring Championship",
        )

        repository.saveAndFlush(tournament)
        entityManager.clear()

        val found = assertNotNull(
            repository.findBySlug("spring-championship"),
        )

        assertNotSame(tournament, found)

        assertEquals(tournament.id, found.id)
        assertEquals("Spring Championship", found.name)
    }

    @Test
    fun `updates timestamp when a managed tournament changes`() {
        val originalUpdatedAt = Instant.parse("2000-01-01T00:00:00Z")
        val tournament = repository.saveAndFlush(
            Tournament(
                slug = "winter-championship",
                name = "Winter Championship",
                updatedAt = originalUpdatedAt,
            ),
        )

        tournament.name = "Winter Open"
        entityManager.flush()
        entityManager.clear()

        val found = assertNotNull(repository.findById(tournament.id).orElse(null))

        assertEquals("Winter Open", found.name)
        assertTrue(found.updatedAt.isAfter(originalUpdatedAt))
    }
}
