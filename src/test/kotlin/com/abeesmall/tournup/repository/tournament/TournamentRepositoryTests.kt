package com.abeesmall.tournup.repository.tournament

import com.abeesmall.tournup.TestcontainersConfiguration
import com.abeesmall.tournup.model.tournament.Tournament
import jakarta.persistence.EntityManager
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNotSame
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase
import org.springframework.context.annotation.Import

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
    fun `updates a managed tournament`() {
        val tournament = repository.saveAndFlush(
            Tournament(
                slug = "winter-championship",
                name = "Winter Championship",
            ),
        )

        tournament.name = "Winter Open"
        entityManager.flush()
        entityManager.clear()

        val found = assertNotNull(repository.findById(tournament.id).orElse(null))

        assertEquals("Winter Open", found.name)
    }
}
