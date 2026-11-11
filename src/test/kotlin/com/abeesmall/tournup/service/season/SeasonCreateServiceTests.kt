package com.abeesmall.tournup.service.season

import com.abeesmall.tournup.dto.season.SeasonCreateRequest
import com.abeesmall.tournup.dto.season.SeasonData
import com.abeesmall.tournup.model.season.Season
import com.abeesmall.tournup.model.tournament.Tournament
import com.abeesmall.tournup.repository.season.SeasonRepository
import com.abeesmall.tournup.repository.tournament.TournamentRepository
import jakarta.validation.ConstraintViolationException
import jakarta.validation.Validation
import java.sql.SQLException
import java.util.Optional
import java.util.UUID
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNotNull
import kotlin.test.assertSame
import org.hibernate.exception.ConstraintViolationException as DatabaseConstraintViolationException
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.`when` as whenever
import org.mockito.Mockito.mock
import org.mockito.Mockito.verify
import org.mockito.Mockito.verifyNoInteractions
import org.mockito.Mockito.verifyNoMoreInteractions
import org.springframework.dao.DataIntegrityViolationException

class SeasonCreateServiceTests {
    private val tournaments = mock(TournamentRepository::class.java)
    private val seasons = mock(SeasonRepository::class.java)
    private val validatorFactory = Validation.buildDefaultValidatorFactory()
    private val service = SeasonCreateService(tournaments, seasons, validatorFactory.validator)
    private val tournament = Tournament(slug = "winter-open", name = "Winter Open")
    private val request = SeasonCreateRequest(name = "2026 S4", slug = "2026-s4", fromYear = 2026)

    @AfterEach
    fun closeValidatorFactory() {
        validatorFactory.close()
    }

    @Test
    fun `form groups existing names by year for the requested tournament`() {
        whenever(tournaments.findById(tournament.id)).thenReturn(Optional.of(tournament))
        whenever(seasons.findAllByTournamentIdOrderByFromYearAscNameAsc(tournament.id))
            .thenReturn(
                listOf(
                    season("2026 S1", "2026-s1", 2026),
                    season("2026 S3", "2026-s3", 2026),
                    season("Winter league", "winter-league", 2026),
                    season("2027 S2", "2027-s2", 2027),
                ),
            )

        val response = service.form(tournament.id)

        assertEquals(tournament.id, response.context.tournamentId)
        assertEquals(
            mapOf(
                2026 to listOf("2026 S1", "2026 S3", "Winter league"),
                2027 to listOf("2027 S2"),
            ),
            response.context.existingNamesByYear,
        )
        assertEquals(SeasonCreateRequest("", "", null), response.item)
        assertEquals(
            "/api/tournaments/${tournament.id}/seasons/",
            response.manifest.commands.single().href,
        )
        verify(tournaments).findById(tournament.id)
        verify(seasons).findAllByTournamentIdOrderByFromYearAscNameAsc(tournament.id)
        verifyNoMoreInteractions(tournaments, seasons)
    }

    @Test
    fun `empty tournament has empty naming context`() {
        whenever(tournaments.findById(tournament.id)).thenReturn(Optional.of(tournament))
        whenever(seasons.findAllByTournamentIdOrderByFromYearAscNameAsc(tournament.id))
            .thenReturn(emptyList())

        assertEquals(emptyMap(), service.form(tournament.id).context.existingNamesByYear)
    }

    @Test
    fun `unknown tournament has no form and does not query seasons`() {
        whenever(tournaments.findById(tournament.id)).thenReturn(Optional.empty())

        assertFailsWith<SeasonTournamentNotFoundException> { service.form(tournament.id) }

        verifyNoInteractions(seasons)
    }

    @Test
    fun `creates a season with the requested parent and editable custom values`() {
        whenever(tournaments.findById(tournament.id)).thenReturn(Optional.of(tournament))
        var persisted: Season? = null
        whenever(seasons.saveAndFlush(anySeason())).thenAnswer { call ->
            call.getArgument<Season>(0).also { persisted = it }
        }
        val custom = request.copy(name = "Summer league", slug = "custom-summer-league")

        val response = service.create(tournament.id, custom)
        val saved = assertNotNull(persisted)

        assertSame(tournament, saved.tournament)
        assertEquals(custom.name, saved.name)
        assertEquals(custom.slug, saved.slug)
        assertEquals(2026, saved.fromYear)
        assertEquals(
            SeasonData(saved.id, tournament.id, custom.name, custom.slug, 2026),
            response.item,
        )
        assertEquals(tournament.id, response.context.tournament.id)
        assertEquals(tournament.name, response.context.tournament.name)
        assertEquals("/api/seasons/${saved.id}/edit/", response.manifest.commands.single().href)
        verify(tournaments).findById(tournament.id)
        verify(seasons).existsByTournamentIdAndName(tournament.id, custom.name)
        verify(seasons).existsByTournamentIdAndSlug(tournament.id, custom.slug)
        verify(seasons).saveAndFlush(saved)
        verifyNoMoreInteractions(tournaments, seasons)
    }

    @Test
    fun `invalid fields are rejected before any persistence call`() {
        val cases = listOf(
            "name" to request.copy(name = "   "),
            "name" to request.copy(name = "x".repeat(256)),
            "slug" to request.copy(slug = ""),
            "slug" to request.copy(slug = "Invalid_Slug"),
            "slug" to request.copy(slug = "x".repeat(256)),
            "fromYear" to request.copy(fromYear = null),
        )

        for ((field, invalid) in cases) {
            val failure = assertFailsWith<ConstraintViolationException> {
                service.create(tournament.id, invalid)
            }
            assertEquals(
                setOf(field),
                failure.constraintViolations.map { it.propertyPath.toString() }.toSet(),
            )
        }
        verifyNoInteractions(tournaments, seasons)
    }

    @Test
    fun `unknown tournament cannot receive a season`() {
        whenever(tournaments.findById(tournament.id)).thenReturn(Optional.empty())

        assertFailsWith<SeasonTournamentNotFoundException> {
            service.create(tournament.id, request)
        }

        verifyNoInteractions(seasons)
    }

    @Test
    fun `existing name in the same tournament is rejected before saving`() {
        whenever(tournaments.findById(tournament.id)).thenReturn(Optional.of(tournament))
        whenever(seasons.existsByTournamentIdAndName(tournament.id, request.name))
            .thenReturn(true)

        val failure = assertFailsWith<SeasonFieldConflictException> {
            service.create(tournament.id, request)
        }

        assertEquals("name", failure.field)
        verify(seasons).existsByTournamentIdAndName(tournament.id, request.name)
        verifyNoMoreInteractions(seasons)
    }

    @Test
    fun `existing slug in the same tournament is rejected before saving`() {
        whenever(tournaments.findById(tournament.id)).thenReturn(Optional.of(tournament))
        whenever(seasons.existsByTournamentIdAndSlug(tournament.id, request.slug))
            .thenReturn(true)

        val failure = assertFailsWith<SeasonFieldConflictException> {
            service.create(tournament.id, request)
        }

        assertEquals("slug", failure.field)
        verify(seasons).existsByTournamentIdAndName(tournament.id, request.name)
        verify(seasons).existsByTournamentIdAndSlug(tournament.id, request.slug)
        verifyNoMoreInteractions(seasons)
    }

    @ParameterizedTest
    @ValueSource(strings = ["name", "slug"])
    fun `database uniqueness conflicts are translated for concurrent submissions`(field: String) {
        whenever(tournaments.findById(tournament.id)).thenReturn(Optional.of(tournament))
        val failure = constraintFailure("seasons_tournament_${field}_unique")
        whenever(seasons.saveAndFlush(anySeason())).thenThrow(failure)

        val conflict = assertFailsWith<SeasonFieldConflictException> {
            service.create(tournament.id, request)
        }

        assertEquals(field, conflict.field)
        assertSame(failure, conflict.cause)
    }

    @Test
    fun `unrelated persistence failures are preserved`() {
        whenever(tournaments.findById(tournament.id)).thenReturn(Optional.of(tournament))
        val failure = constraintFailure("seasons_tournament_fk")
        whenever(seasons.saveAndFlush(anySeason())).thenThrow(failure)

        assertSame(
            failure,
            assertFailsWith<DataIntegrityViolationException> {
                service.create(tournament.id, request)
            },
        )
    }

    private fun season(name: String, slug: String, year: Int): Season =
        Season(tournament = tournament, name = name, slug = slug, fromYear = year)

    // Java Mockito matchers return null; Kotlin needs a non-null placeholder.
    private fun anySeason(): Season =
        any(Season::class.java) ?: season("Placeholder", "placeholder", 2026)

    private fun constraintFailure(name: String): DataIntegrityViolationException =
        DataIntegrityViolationException(
            "Constraint failed",
            DatabaseConstraintViolationException(
                "Constraint failed",
                SQLException("Constraint failed", "23505"),
                name,
            ),
        )
}
