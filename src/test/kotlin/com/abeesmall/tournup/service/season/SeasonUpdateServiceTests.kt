package com.abeesmall.tournup.service.season

import com.abeesmall.tournup.dto.season.SeasonData
import com.abeesmall.tournup.dto.season.SeasonWriteRequest
import com.abeesmall.tournup.model.season.Season
import com.abeesmall.tournup.model.tournament.Tournament
import com.abeesmall.tournup.repository.season.SeasonRepository
import com.abeesmall.tournup.repository.tournament.TournamentRepository
import jakarta.validation.ConstraintViolationException
import jakarta.validation.Validation
import java.sql.SQLException
import java.util.Optional
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertSame
import org.hibernate.exception.ConstraintViolationException as DatabaseConstraintViolationException
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import org.mockito.Mockito.`when` as whenever
import org.mockito.Mockito.mock
import org.mockito.Mockito.times
import org.mockito.Mockito.verify
import org.mockito.Mockito.verifyNoInteractions
import org.mockito.Mockito.verifyNoMoreInteractions
import org.springframework.dao.DataIntegrityViolationException

class SeasonUpdateServiceTests {
    private val seasons = mock(SeasonRepository::class.java)
    private val validatorFactory = Validation.buildDefaultValidatorFactory()
    private val service = SeasonService(mock(TournamentRepository::class.java), seasons, validatorFactory.validator)
    private val tournament = Tournament(slug = "winter-open", name = "Winter Open")
    private val existing = Season(
        tournament = tournament,
        name = "2026 S1",
        slug = "2026-s1",
        fromYear = 2026,
    )
    private val request = SeasonWriteRequest("Summer league", "summer-league", 2027, tournament.locator)

    @AfterEach
    fun closeValidatorFactory() {
        validatorFactory.close()
    }

    @Test
    fun `NanoID cannot be changed after creation`() {
        whenever(seasons.findByNanoId(existing.nanoId)).thenReturn(existing)
        val conflict = assertFailsWith<org.springframework.web.server.ResponseStatusException> {
            service.update(existing.locator, request.copy(nanoId = if (existing.nanoId == "BCDF") "GHJK" else "BCDF"))
        }
        assertEquals(org.springframework.http.HttpStatus.BAD_REQUEST, conflict.statusCode)
        verify(seasons).findByNanoId(existing.nanoId)
        verifyNoMoreInteractions(seasons)
    }

    @Test
    fun `a renamed parent remains the same immutable relationship`() {
        val old = request
        tournament.slug = "renamed-parent"
        whenever(seasons.findByNanoId(existing.nanoId)).thenReturn(existing)
        whenever(seasons.saveAndFlush(existing)).thenReturn(existing)
        val result = assertNotNull(service.update(existing.locator, old))
        assertSame(tournament, existing.tournament)
        assertEquals(tournament.locator, result.item.tournamentId)
    }

    @Test
    fun `updates editable fields and preserves identity and tournament`() {
        whenever(seasons.findByNanoId(existing.nanoId)).thenReturn(existing)
        whenever(seasons.saveAndFlush(existing)).thenReturn(existing)

        val result = assertNotNull(service.update(existing.locator, request))

        assertEquals(request.name, existing.name)
        assertEquals(request.slug, existing.slug)
        assertEquals(request.fromYear, existing.fromYear)
        assertSame(tournament, existing.tournament)
        assertEquals(
            SeasonData(existing.id, tournament.locator, request.name, request.slug, 2027, nanoId = existing.nanoId),
            result.item,
        )
        assertEquals(
            mapOf("tournament" to listOf(mapOf("id" to tournament.locator, "name" to tournament.name))),
            result.included,
        )
        verify(seasons).findByNanoId(existing.nanoId)
        verify(seasons).existsByTournamentIdAndNameAndIdNot(tournament.id, request.name, existing.id)
        verify(seasons).saveAndFlush(existing)
        verifyNoMoreInteractions(seasons)
    }

    @Test
    fun `edit form includes the stored tournament reference`() {
        whenever(seasons.findByNanoId(existing.nanoId)).thenReturn(existing)

        val response = assertNotNull(service.editForm(existing.locator))

        assertEquals(SeasonWriteRequest("2026 S1", "2026-s1", 2026, tournament.locator, existing.nanoId), response.item)
        assertEquals(
            mapOf("tournament" to listOf(mapOf("id" to tournament.locator, "name" to tournament.name))),
            response.included,
        )
        verify(seasons).findByNanoId(existing.nanoId)
        verifyNoMoreInteractions(seasons)
    }

    @Test
    fun `missing season has no edit form`() {
        whenever(seasons.findByNanoId(existing.nanoId)).thenReturn(null)

        assertNull(service.editForm(existing.locator))

        verify(seasons).findByNanoId(existing.nanoId)
        verifyNoMoreInteractions(seasons)
    }

    @Test
    fun `nested requests with the wrong parent neither expose a form nor change the season`() {
        whenever(seasons.findByNanoId(existing.nanoId)).thenReturn(existing)
        val wrongParent = "missing-ZZZZ"

        assertFailsWith<com.abeesmall.tournup.service.season.SeasonTournamentNotFoundException> { service.editForm(existing.locator, wrongParent) }
        assertFailsWith<com.abeesmall.tournup.service.season.SeasonTournamentNotFoundException> { service.update(existing.locator, request, wrongParent) }

        assertEquals("2026 S1", existing.name)
        assertEquals("2026-s1", existing.slug)
        assertEquals(2026, existing.fromYear)
        assertSame(tournament, existing.tournament)
        verify(seasons, times(2)).findByNanoId(existing.nanoId)
        verifyNoMoreInteractions(seasons)
    }

    @Test
    fun `allows changing the year while keeping the current name and slug`() {
        val unchanged = SeasonWriteRequest(existing.name, existing.slug, 2027, tournament.locator)
        whenever(seasons.findByNanoId(existing.nanoId)).thenReturn(existing)
        whenever(seasons.saveAndFlush(existing)).thenReturn(existing)

        val response = assertNotNull(service.update(existing.locator, unchanged))
        assertEquals("2026 S1", response.item.name)
        assertEquals("2026-s1", response.item.slug)
        assertEquals(2027, response.item.fromYear)

        verify(seasons).existsByTournamentIdAndNameAndIdNot(tournament.id, unchanged.name, existing.id)
        verify(seasons).saveAndFlush(existing)
    }

    @Test
    fun `unknown season returns null without saving`() {
        whenever(seasons.findByNanoId(existing.nanoId)).thenReturn(null)

        assertNull(service.update(existing.locator, request))

        verify(seasons).findByNanoId(existing.nanoId)
        verifyNoMoreInteractions(seasons)
    }

    @Test
    fun `invalid fields are rejected without persistence calls`() {
        val cases = listOf(
            "name" to request.copy(name = "   "),
            "name" to request.copy(name = "x".repeat(256)),
            "slug" to request.copy(slug = ""),
            "slug" to request.copy(slug = "Invalid_Slug"),
            "slug" to request.copy(slug = "x".repeat(256)),
            "fromYear" to request.copy(fromYear = null),
            "tournamentId" to request.copy(tournamentId = null),
        )
        for ((field, invalid) in cases) {
            val failure = assertFailsWith<ConstraintViolationException> {
                service.update(existing.locator, invalid)
            }
            assertEquals(
                setOf(field),
                failure.constraintViolations.map { it.propertyPath.toString() }.toSet(),
            )
        }
        verifyNoInteractions(seasons)
    }

    @Test
    fun `a different submitted tournament is rejected before changing any fields`() {
        whenever(seasons.findByNanoId(existing.nanoId)).thenReturn(existing)

        assertFailsWith<SeasonTournamentChangeException> {
            service.update(existing.locator, request.copy(tournamentId = "missing-ZZZZ"))
        }

        assertSame(tournament, existing.tournament)
        assertEquals("2026 S1", existing.name)
        assertEquals("2026-s1", existing.slug)
        assertEquals(2026, existing.fromYear)
        verify(seasons).findByNanoId(existing.nanoId)
        verifyNoMoreInteractions(seasons)
    }

    @ParameterizedTest
    @ValueSource(strings = ["name"])
    fun `another season's values are rejected before changing the entity`(field: String) {
        whenever(seasons.findByNanoId(existing.nanoId)).thenReturn(existing)
        if (field == "name") {
            whenever(seasons.existsByTournamentIdAndNameAndIdNot(tournament.id, request.name, existing.id))
                .thenReturn(true)
        }

        val conflict = assertFailsWith<SeasonFieldConflictException> {
            service.update(existing.locator, request)
        }

        assertEquals(field, conflict.field)
        assertEquals("2026 S1", existing.name)
        assertEquals("2026-s1", existing.slug)
        assertEquals(2026, existing.fromYear)
        assertSame(tournament, existing.tournament)
        verify(seasons).findByNanoId(existing.nanoId)
        verify(seasons).existsByTournamentIdAndNameAndIdNot(tournament.id, request.name, existing.id)
        verifyNoMoreInteractions(seasons)
    }

    @ParameterizedTest
    @ValueSource(strings = ["name"])
    fun `concurrent database uniqueness failures are translated`(field: String) {
        whenever(seasons.findByNanoId(existing.nanoId)).thenReturn(existing)
        val failure = constraintFailure("seasons_tournament_${field}_unique")
        whenever(seasons.saveAndFlush(existing)).thenThrow(failure)

        val conflict = assertFailsWith<SeasonFieldConflictException> {
            service.update(existing.locator, request)
        }

        assertEquals(field, conflict.field)
        assertSame(failure, conflict.cause)
    }

    @Test
    fun `unrelated persistence failures are preserved`() {
        whenever(seasons.findByNanoId(existing.nanoId)).thenReturn(existing)
        val failure = constraintFailure("seasons_tournament_fk")
        whenever(seasons.saveAndFlush(existing)).thenThrow(failure)

        assertSame(
            failure,
            assertFailsWith<DataIntegrityViolationException> { service.update(existing.locator, request) },
        )
    }

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
