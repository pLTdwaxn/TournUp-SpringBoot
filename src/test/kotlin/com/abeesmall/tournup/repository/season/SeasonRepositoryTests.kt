package com.abeesmall.tournup.repository.season

import com.abeesmall.tournup.TestcontainersConfiguration
import com.abeesmall.tournup.model.season.Season
import com.abeesmall.tournup.model.tournament.Tournament
import com.abeesmall.tournup.repository.audit.LatestAuditReader
import com.abeesmall.tournup.repository.tournament.TournamentRepository
import jakarta.persistence.EntityManager
import java.util.UUID
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase
import org.springframework.context.annotation.Import
import org.springframework.dao.DataIntegrityViolationException
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Sort
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.transaction.PlatformTransactionManager
import org.springframework.transaction.annotation.Propagation
import org.springframework.transaction.annotation.Transactional
import org.springframework.transaction.support.TransactionTemplate

@DataJpaTest
@AutoConfigureTestDatabase(
    replace = AutoConfigureTestDatabase.Replace.NONE,
)
@Import(TestcontainersConfiguration::class, LatestAuditReader::class)
class SeasonRepositoryTests {
    @Autowired
    lateinit var seasons: SeasonRepository

    @Autowired
    lateinit var tournaments: TournamentRepository

    @Autowired
    lateinit var entityManager: EntityManager

    @Autowired
    lateinit var jdbc: JdbcTemplate

    @Autowired
    lateinit var auditReader: LatestAuditReader

    @Autowired
    lateinit var transactionManager: PlatformTransactionManager

    @Test
    fun `saves minimal season fields and looks up within its tournament`() {
        val tournament = newTournament()
        val saved = seasons.saveAndFlush(
            Season(
                tournament = tournament,
                name = "2026 S1",
                slug = "2026-s1",
                fromYear = 2026,
            ),
        )
        entityManager.clear()

        val found = assertNotNull(
            seasons.findByTournamentIdAndSlug(tournament.id, "2026-s1"),
        )

        assertEquals(saved.id, found.id)
        assertEquals(tournament.id, found.tournament.id)
        assertEquals("2026 S1", found.name)
        assertEquals("2026-s1", found.slug)
        assertEquals(2026, found.fromYear)
        assertNull(
            seasons.findByTournamentIdAndSlug(UUID.randomUUID(), "2026-s1"),
        )
    }

    @Test
    fun `same name and slug can be used in different tournaments`() {
        val firstTournament = newTournament()
        val secondTournament = newTournament()

        val first = seasons.saveAndFlush(newSeason(firstTournament))
        val second = seasons.saveAndFlush(newSeason(secondTournament))

        assertTrue(first.id != second.id)
        assertEquals(
            first.id,
            seasons.findByTournamentIdAndSlug(firstTournament.id, "2026-s1")?.id,
        )
        assertEquals(
            second.id,
            seasons.findByTournamentIdAndSlug(secondTournament.id, "2026-s1")?.id,
        )
    }

    @Test
    fun `merging detached season data cannot change its persisted tournament`() {
        val original = newTournament()
        val other = newTournament()
        val saved = seasons.saveAndFlush(newSeason(original))
        entityManager.clear()

        seasons.saveAndFlush(
            Season(
                id = saved.id,
                tournament = other,
                name = "Updated season",
                slug = "updated-season",
                fromYear = 2027,
            ),
        )
        entityManager.clear()

        val reloaded = seasons.findById(saved.id).orElseThrow()
        assertEquals(original.id, reloaded.tournament.id)
        assertEquals("Updated season", reloaded.name)
        assertEquals("updated-season", reloaded.slug)
        assertEquals(2027, reloaded.fromYear)
    }

    @Test
    fun `creation queries include only the requested tournament`() {
        val tournament = newTournament()
        val other = newTournament()
        val later = seasons.saveAndFlush(newSeason(tournament, fromYear = 2027))
        val earlier = seasons.saveAndFlush(
            newSeason(tournament, name = "2026 S2", slug = "2026-s2"),
        )
        seasons.saveAndFlush(newSeason(other, name = "Other season", slug = "other-season"))
        entityManager.clear()

        assertEquals(
            listOf(earlier.id, later.id),
            seasons.findAllByTournamentIdOrderByFromYearAscNameAsc(tournament.id)
                .map { it.id },
        )
        assertTrue(seasons.existsByTournamentIdAndName(tournament.id, earlier.name))
        assertTrue(seasons.existsByTournamentIdAndSlug(tournament.id, earlier.slug))
        assertFalse(seasons.existsByTournamentIdAndName(tournament.id, "Other season"))
        assertFalse(seasons.existsByTournamentIdAndSlug(tournament.id, "other-season"))
        assertFalse(seasons.existsByTournamentIdAndName(other.id, earlier.name))
        assertFalse(seasons.existsByTournamentIdAndSlug(other.id, earlier.slug))
    }

    @Test
    fun `pagination and totals are scoped to the requested tournament`() {
        val tournament = newTournament()
        val other = newTournament()
        val earlier = seasons.saveAndFlush(newSeason(tournament))
        val later = seasons.saveAndFlush(
            newSeason(tournament, name = "2027 S1", slug = "2027-s1", fromYear = 2027),
        )
        seasons.saveAndFlush(newSeason(other, fromYear = 2028))
        entityManager.clear()
        val sort = Sort.by(
            Sort.Order.desc("fromYear"), Sort.Order.asc("name"), Sort.Order.asc("id"),
        )

        val firstPage = seasons.findAllByTournamentId(tournament.id, PageRequest.of(0, 1, sort))
        val secondPage = seasons.findAllByTournamentId(tournament.id, PageRequest.of(1, 1, sort))

        assertEquals(listOf(later.id), firstPage.content.map { it.id })
        assertEquals(listOf(earlier.id), secondPage.content.map { it.id })
        assertEquals(2L, firstPage.totalElements)
        assertEquals(2L, secondPage.totalElements)
        assertEquals(2, firstPage.totalPages)
        val missing = seasons.findAllByTournamentId(UUID.randomUUID(), PageRequest.of(0, 1, sort))
        assertTrue(missing.isEmpty)
        assertEquals(0L, missing.totalElements)
    }

    @Test
    fun `update conflict checks exclude only this season within its tournament`() {
        val tournament = newTournament()
        val other = newTournament()
        val current = seasons.saveAndFlush(newSeason(tournament))
        val sibling = seasons.saveAndFlush(
            newSeason(tournament, name = "2026 S2", slug = "2026-s2", fromYear = 2027),
        )
        val unrelated = seasons.saveAndFlush(
            newSeason(other, name = "2027 S1", slug = "2027-s1", fromYear = 2027),
        )

        assertFalse(seasons.existsByTournamentIdAndNameAndIdNot(tournament.id, current.name, current.id))
        assertFalse(seasons.existsByTournamentIdAndSlugAndIdNot(tournament.id, current.slug, current.id))
        assertTrue(seasons.existsByTournamentIdAndNameAndIdNot(tournament.id, sibling.name, current.id))
        assertTrue(seasons.existsByTournamentIdAndSlugAndIdNot(tournament.id, sibling.slug, current.id))
        assertFalse(seasons.existsByTournamentIdAndNameAndIdNot(tournament.id, unrelated.name, current.id))
        assertFalse(seasons.existsByTournamentIdAndSlugAndIdNot(tournament.id, unrelated.slug, current.id))
    }

    @Test
    fun `duplicate name is rejected within the same tournament`() {
        val tournament = newTournament()
        seasons.saveAndFlush(newSeason(tournament))

        assertFailsWith<DataIntegrityViolationException> {
            seasons.saveAndFlush(
                newSeason(tournament, slug = "another-slug", fromYear = 2027),
            )
        }
    }

    @Test
    fun `duplicate slug is rejected within the same tournament`() {
        val tournament = newTournament()
        seasons.saveAndFlush(newSeason(tournament))

        assertFailsWith<DataIntegrityViolationException> {
            seasons.saveAndFlush(
                newSeason(tournament, name = "Another season", fromYear = 2027),
            )
        }
    }

    @Test
    fun `database rejects a reference to an unknown tournament`() {
        assertFailsWith<DataIntegrityViolationException> {
            jdbc.update(
                """
                INSERT INTO seasons (id, tournament_id, name, slug, from_year)
                VALUES (?, ?, ?, ?, ?)
                """.trimIndent(),
                UUID.randomUUID(),
                UUID.randomUUID(),
                "2026 S1",
                "2026-s1",
                2026,
            )
        }
    }

    @ParameterizedTest
    @ValueSource(strings = ["id", "tournament_id", "name", "slug", "from_year"])
    fun `database requires every season field`(column: String) {
        val tournament = newTournament()
        val values = mutableMapOf<String, Any?>(
            "id" to UUID.randomUUID(),
            "tournament_id" to tournament.id,
            "name" to "2026 S1",
            "slug" to "2026-s1",
            "from_year" to 2026,
        )
        values[column] = null

        assertFailsWith<DataIntegrityViolationException> {
            jdbc.update(
                """
                INSERT INTO seasons (id, tournament_id, name, slug, from_year)
                VALUES (?, ?, ?, ?, ?)
                """.trimIndent(),
                values["id"],
                values["tournament_id"],
                values["name"],
                values["slug"],
                values["from_year"],
            )
        }
    }

    @Test
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    fun `committed season creation and updates produce audit revisions`() {
        val transaction = TransactionTemplate(transactionManager)
        val saved = assertNotNull(
            transaction.execute {
                seasons.saveAndFlush(newSeason(newTournament()))
            },
        )
        val createdAudit = assertNotNull(
            auditReader.findLatest(Season::class.java, listOf(saved.id))[saved.id],
        )

        transaction.executeWithoutResult {
            val season = seasons.findById(saved.id).orElseThrow()
            season.name = "2027 S1"
            season.slug = "2027-s1"
            season.fromYear = 2027
        }

        val updatedAudit = assertNotNull(
            auditReader.findLatest(Season::class.java, listOf(saved.id))[saved.id],
        )
        assertTrue(updatedAudit.revision > createdAudit.revision)
    }

    private fun newTournament(): Tournament =
        tournaments.saveAndFlush(
            Tournament(
                slug = "tournament-${UUID.randomUUID()}",
                name = "Tournament",
            ),
        )

    private fun newSeason(
        tournament: Tournament,
        name: String = "2026 S1",
        slug: String = "2026-s1",
        fromYear: Int = 2026,
    ): Season = Season(
        tournament = tournament,
        name = name,
        slug = slug,
        fromYear = fromYear,
    )
}
