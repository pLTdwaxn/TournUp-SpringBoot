package com.abeesmall.tournup.service.tournament

import com.abeesmall.tournup.dto.tournament.TournamentData
import com.abeesmall.tournup.dto.tournament.TournamentUpdateRequest
import com.abeesmall.tournup.model.tournament.HandicapMode
import com.abeesmall.tournup.model.tournament.ParticipantType
import com.abeesmall.tournup.model.tournament.ParticipationMode
import com.abeesmall.tournup.model.tournament.Tournament
import com.abeesmall.tournup.repository.tournament.TournamentRepository
import jakarta.validation.Validation
import java.sql.SQLException
import java.util.Optional
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertSame
import org.hibernate.exception.ConstraintViolationException
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Test
import org.mockito.Mockito.`when` as whenever
import org.mockito.Mockito.mock
import org.mockito.Mockito.verify
import org.mockito.Mockito.verifyNoMoreInteractions
import org.springframework.dao.DataIntegrityViolationException

class TournamentUpdateServiceTests {
    private val tournaments = mock(TournamentRepository::class.java)
    private val validatorFactory = Validation.buildDefaultValidatorFactory()
    private val service = TournamentService(tournaments, validatorFactory.validator)

    @AfterEach
    fun closeValidatorFactory() = validatorFactory.close()

    private val existing = Tournament(
        slug = "original",
        name = "Original tournament",
        description = "Original description",
    )

    private val request = TournamentUpdateRequest(
        name = "Updated tournament",
        slug = "updated",
        description = null,
        participantType = ParticipantType.TEAM,
        participationMode = ParticipationMode.REGISTRATION,
        handicapMode = HandicapMode.CARRY_FORWARD,
    )

    @Test
    fun `invalid direct updates fail before loading or changing records`() {
        for (invalid in listOf(request.copy(name = " "), request.copy(slug = "Invalid_Slug"))) {
            assertFailsWith<jakarta.validation.ConstraintViolationException> { service.update(existing.id, invalid) }
        }
        org.mockito.Mockito.verifyNoInteractions(tournaments)
    }

    @Test
    fun `replaces editable fields while preserving the id`() {
        whenever(tournaments.findById(existing.id))
            .thenReturn(Optional.of(existing))
        whenever(tournaments.saveAndFlush(existing)).thenReturn(existing)

        val response = assertNotNull(service.update(existing.id, request))

        assertEquals(request.name, existing.name)
        assertEquals(request.slug, existing.slug)
        assertNull(existing.description)
        assertEquals(request.participantType, existing.participantType)
        assertEquals(request.participationMode, existing.participationMode)
        assertEquals(request.handicapMode, existing.handicapMode)

        assertEquals(
            TournamentData(
                id = existing.id,
                name = request.name,
                slug = request.slug,
                description = request.description,
                participantType = request.participantType,
                participationMode = request.participationMode,
                handicapMode = request.handicapMode,
            ),
            response.item,
        )

        verify(tournaments).findById(existing.id)
        verify(tournaments).findBySlug(request.slug)
        verify(tournaments).saveAndFlush(existing)
        verifyNoMoreInteractions(tournaments)
    }

    @Test
    fun `allows keeping the current slug`() {
        val unchangedSlug = request.copy(slug = existing.slug)
        whenever(tournaments.findById(existing.id))
            .thenReturn(Optional.of(existing))
        whenever(tournaments.findBySlug(existing.slug)).thenReturn(existing)
        whenever(tournaments.saveAndFlush(existing)).thenReturn(existing)

        assertNotNull(service.update(existing.id, unchangedSlug))

        verify(tournaments).saveAndFlush(existing)
    }

    @Test
    fun `returns null for a missing tournament without saving`() {
        whenever(tournaments.findById(existing.id))
            .thenReturn(Optional.empty())

        assertNull(service.update(existing.id, request))

        verify(tournaments).findById(existing.id)
        verifyNoMoreInteractions(tournaments)
    }

    @Test
    fun `rejects another tournament's slug before changing data`() {
        whenever(tournaments.findById(existing.id))
            .thenReturn(Optional.of(existing))
        whenever(tournaments.findBySlug(request.slug)).thenReturn(
            Tournament(slug = request.slug, name = "Another tournament"),
        )

        assertFailsWith<TournamentSlugConflictException> {
            service.update(existing.id, request)
        }

        assertEquals("original", existing.slug)
        assertEquals("Original tournament", existing.name)
        verify(tournaments).findById(existing.id)
        verify(tournaments).findBySlug(request.slug)
        verifyNoMoreInteractions(tournaments)
    }

    @Test
    fun `translates a database slug constraint failure`() {
        val failure = constraintFailure("tournaments_slug_unique")
        whenever(tournaments.findById(existing.id))
            .thenReturn(Optional.of(existing))
        whenever(tournaments.saveAndFlush(existing)).thenThrow(failure)

        val conflict = assertFailsWith<TournamentSlugConflictException> {
            service.update(existing.id, request)
        }

        assertSame(failure, conflict.cause)
    }

    @Test
    fun `preserves unrelated persistence failures`() {
        val failure = constraintFailure("another_unique_constraint")
        whenever(tournaments.findById(existing.id))
            .thenReturn(Optional.of(existing))
        whenever(tournaments.saveAndFlush(existing)).thenThrow(failure)

        val thrown = assertFailsWith<DataIntegrityViolationException> {
            service.update(existing.id, request)
        }

        assertSame(failure, thrown)
    }

    @Test
    fun `edit form contains the stored values`() {
        existing.participantType = ParticipantType.TEAM
        existing.participationMode = ParticipationMode.REGISTRATION
        existing.handicapMode = HandicapMode.CARRY_FORWARD

        whenever(tournaments.findById(existing.id))
            .thenReturn(Optional.of(existing))

        val response = assertNotNull(service.editForm(existing.id))

        assertEquals(
            TournamentUpdateRequest(
                name = "Original tournament",
                slug = "original",
                description = "Original description",
                participantType = ParticipantType.TEAM,
                participationMode = ParticipationMode.REGISTRATION,
                handicapMode = HandicapMode.CARRY_FORWARD,
            ),
            response.item,
        )

        verify(tournaments).findById(existing.id)
        verifyNoMoreInteractions(tournaments)
    }

    @Test
    fun `edit form returns null for a missing tournament`() {
        whenever(tournaments.findById(existing.id))
            .thenReturn(Optional.empty())

        assertNull(service.editForm(existing.id))

        verify(tournaments).findById(existing.id)
        verifyNoMoreInteractions(tournaments)
    }

    private fun constraintFailure(name: String): DataIntegrityViolationException =
        DataIntegrityViolationException(
            "Constraint failed",
            ConstraintViolationException(
                "Constraint failed",
                SQLException("Constraint failed", "23505"),
                name,
            ),
        )
}
