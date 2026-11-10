package com.abeesmall.tournup.service.season

import com.abeesmall.tournup.dto.season.SeasonData
import com.abeesmall.tournup.dto.season.SeasonUpdateRequest
import com.abeesmall.tournup.model.season.Season
import com.abeesmall.tournup.model.tournament.Tournament
import com.abeesmall.tournup.repository.season.SeasonRepository
import jakarta.validation.ConstraintViolationException
import jakarta.validation.Validation
import java.sql.SQLException
import java.util.Optional
import java.util.UUID
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
    private val service = SeasonUpdateService(seasons, validatorFactory.validator)
    private val tournament = Tournament(slug = "winter-open", name = "Winter Open")
    private val existing = Season(
        tournament = tournament,
        name = "2026 S1",
        slug = "2026-s1",
        fromYear = 2026,
    )
    private val request = SeasonUpdateRequest("Summer league", "summer-league", 2027)

    @AfterEach
    fun closeValidatorFactory() {
        validatorFactory.close()
    }

    @Test
    fun `updates editable fields and preserves identity and tournament`() {
        whenever(seasons.findById(existing.id)).thenReturn(Optional.of(existing))
        whenever(seasons.saveAndFlush(existing)).thenReturn(existing)

        val result = assertNotNull(service.update(existing.id, request))

        assertEquals(request.name, existing.name)
        assertEquals(request.slug, existing.slug)
        assertEquals(request.fromYear, existing.fromYear)
        assertSame(tournament, existing.tournament)
        assertEquals(
            SeasonData(existing.id, tournament.id, request.name, request.slug, 2027),
            result.item,
        )
        assertEquals(tournament.name, result.context?.tournament?.name)
        verify(seasons).findById(existing.id)
        verify(seasons).existsByTournamentIdAndNameAndIdNot(tournament.id, request.name, existing.id)
        verify(seasons).existsByTournamentIdAndSlugAndIdNot(tournament.id, request.slug, existing.id)
        verify(seasons).saveAndFlush(existing)
        verifyNoMoreInteractions(seasons)
    }

    @Test
    fun `edit form preserves existing values and disables generated suggestions`() {
        whenever(seasons.findById(existing.id)).thenReturn(Optional.of(existing))

        val response = assertNotNull(service.form(existing.id))

        assertEquals(SeasonUpdateRequest("2026 S1", "2026-s1", 2026), response.item)
        assertEquals(tournament.id, response.context.tournament.id)
        assertEquals(tournament.name, response.context.tournament.name)
        assertEquals("/api/seasons/${existing.id}/", response.manifest.commands.single().href)
        assertEquals(emptyList(), response.manifest.fields.getValue("name").behaviours)
        assertEquals(emptyList(), response.manifest.fields.getValue("slug").behaviours)
        verify(seasons).findById(existing.id)
        verifyNoMoreInteractions(seasons)
    }

    @Test
    fun `missing season has no edit form`() {
        whenever(seasons.findById(existing.id)).thenReturn(Optional.empty())

        assertNull(service.form(existing.id))

        verify(seasons).findById(existing.id)
        verifyNoMoreInteractions(seasons)
    }

    @Test
    fun `nested requests with the wrong parent neither expose a form nor change the season`() {
        whenever(seasons.findById(existing.id)).thenReturn(Optional.of(existing))
        val wrongParent = UUID.randomUUID()

        assertNull(service.form(existing.id, wrongParent))
        assertNull(service.update(existing.id, request, wrongParent))

        assertEquals("2026 S1", existing.name)
        assertEquals("2026-s1", existing.slug)
        assertEquals(2026, existing.fromYear)
        assertSame(tournament, existing.tournament)
        verify(seasons, times(2)).findById(existing.id)
        verifyNoMoreInteractions(seasons)
    }

    @Test
    fun `allows changing the year while keeping the current name and slug`() {
        val unchanged = SeasonUpdateRequest(existing.name, existing.slug, 2027)
        whenever(seasons.findById(existing.id)).thenReturn(Optional.of(existing))
        whenever(seasons.saveAndFlush(existing)).thenReturn(existing)

        val response = assertNotNull(service.update(existing.id, unchanged))
        assertEquals("2026 S1", response.item.name)
        assertEquals("2026-s1", response.item.slug)
        assertEquals(2027, response.item.fromYear)

        verify(seasons).existsByTournamentIdAndNameAndIdNot(tournament.id, unchanged.name, existing.id)
        verify(seasons).existsByTournamentIdAndSlugAndIdNot(tournament.id, unchanged.slug, existing.id)
        verify(seasons).saveAndFlush(existing)
    }

    @Test
    fun `unknown season returns null without saving`() {
        whenever(seasons.findById(existing.id)).thenReturn(Optional.empty())

        assertNull(service.update(existing.id, request))

        verify(seasons).findById(existing.id)
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
        )
        for ((field, invalid) in cases) {
            val failure = assertFailsWith<ConstraintViolationException> {
                service.update(existing.id, invalid)
            }
            assertEquals(
                setOf(field),
                failure.constraintViolations.map { it.propertyPath.toString() }.toSet(),
            )
        }
        verifyNoInteractions(seasons)
    }

    @ParameterizedTest
    @ValueSource(strings = ["name", "slug"])
    fun `another season's values are rejected before changing the entity`(field: String) {
        whenever(seasons.findById(existing.id)).thenReturn(Optional.of(existing))
        if (field == "name") {
            whenever(seasons.existsByTournamentIdAndNameAndIdNot(tournament.id, request.name, existing.id))
                .thenReturn(true)
        } else {
            whenever(seasons.existsByTournamentIdAndSlugAndIdNot(tournament.id, request.slug, existing.id))
                .thenReturn(true)
        }

        val conflict = assertFailsWith<SeasonFieldConflictException> {
            service.update(existing.id, request)
        }

        assertEquals(field, conflict.field)
        assertEquals("2026 S1", existing.name)
        assertEquals("2026-s1", existing.slug)
        assertEquals(2026, existing.fromYear)
        assertSame(tournament, existing.tournament)
        verify(seasons).findById(existing.id)
        verify(seasons).existsByTournamentIdAndNameAndIdNot(tournament.id, request.name, existing.id)
        if (field == "slug") {
            verify(seasons).existsByTournamentIdAndSlugAndIdNot(tournament.id, request.slug, existing.id)
        }
        verifyNoMoreInteractions(seasons)
    }

    @ParameterizedTest
    @ValueSource(strings = ["name", "slug"])
    fun `concurrent database uniqueness failures are translated`(field: String) {
        whenever(seasons.findById(existing.id)).thenReturn(Optional.of(existing))
        val failure = constraintFailure("seasons_tournament_${field}_unique")
        whenever(seasons.saveAndFlush(existing)).thenThrow(failure)

        val conflict = assertFailsWith<SeasonFieldConflictException> {
            service.update(existing.id, request)
        }

        assertEquals(field, conflict.field)
        assertSame(failure, conflict.cause)
    }

    @Test
    fun `unrelated persistence failures are preserved`() {
        whenever(seasons.findById(existing.id)).thenReturn(Optional.of(existing))
        val failure = constraintFailure("seasons_tournament_fk")
        whenever(seasons.saveAndFlush(existing)).thenThrow(failure)

        assertSame(
            failure,
            assertFailsWith<DataIntegrityViolationException> { service.update(existing.id, request) },
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
