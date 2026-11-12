package com.abeesmall.tournup.service.tournament

import com.abeesmall.tournup.dto.tournament.TournamentCreateRequest
import com.abeesmall.tournup.dto.tournament.TournamentData
import com.abeesmall.tournup.model.tournament.HandicapMode
import com.abeesmall.tournup.model.tournament.ParticipantType
import com.abeesmall.tournup.model.tournament.ParticipationMode
import com.abeesmall.tournup.model.tournament.Tournament
import com.abeesmall.tournup.repository.tournament.TournamentRepository
import jakarta.validation.Validation
import java.sql.SQLException
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNotNull
import kotlin.test.assertSame
import org.hibernate.exception.ConstraintViolationException
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Test
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.`when` as whenever
import org.mockito.Mockito.mock
import org.mockito.Mockito.verify
import org.mockito.Mockito.verifyNoMoreInteractions
import org.springframework.dao.DataIntegrityViolationException

class TournamentCreateServiceTests {
    private val tournaments = mock(TournamentRepository::class.java)
    private val validatorFactory = Validation.buildDefaultValidatorFactory()
    private val service = TournamentService(tournaments, validatorFactory.validator)

    @AfterEach
    fun closeValidatorFactory() = validatorFactory.close()

    private val request = TournamentCreateRequest(
        name = "Winter Open",
        slug = "winter-open",
        description = "A team competition",
        participantType = ParticipantType.TEAM,
        participationMode = ParticipationMode.REGISTRATION,
        handicapMode = HandicapMode.CARRY_FORWARD,
    )

    @Test
    fun `invalid direct service calls fail before persistence`() {
        for (invalid in listOf(request.copy(name = " "), request.copy(slug = "Invalid_Slug"))) {
            assertFailsWith<jakarta.validation.ConstraintViolationException> { service.create(invalid) }
        }
        org.mockito.Mockito.verifyNoInteractions(tournaments)
    }

    @Test
    fun `saves the requested data and returns its detail response`() {
        var persisted: Tournament? = null
        whenever(tournaments.saveAndFlush(anyTournament())).thenAnswer { call ->
            call.getArgument<Tournament>(0).also { persisted = it }
        }

        val response = service.create(request)
        val saved = assertNotNull(persisted)

        assertEquals(request.name, saved.name)
        assertEquals(request.slug, saved.slug)
        assertEquals(request.description, saved.description)
        assertEquals(request.participantType, saved.participantType)
        assertEquals(request.participationMode, saved.participationMode)
        assertEquals(request.handicapMode, saved.handicapMode)

        assertEquals(
            TournamentData(
                id = saved.id,
                slug = request.slug,
                name = request.name,
                description = request.description,
                participantType = request.participantType,
                participationMode = request.participationMode,
                handicapMode = request.handicapMode,
            ),
            response.item,
        )

        verify(tournaments).findBySlug(request.slug)
        verify(tournaments).saveAndFlush(saved)
        verifyNoMoreInteractions(tournaments)
    }

    @Test
    fun `rejects an existing slug without saving`() {
        whenever(tournaments.findBySlug(request.slug)).thenReturn(
            Tournament(slug = request.slug, name = "Existing tournament"),
        )

        assertFailsWith<TournamentSlugConflictException> {
            service.create(request)
        }

        verify(tournaments).findBySlug(request.slug)
        verifyNoMoreInteractions(tournaments)
    }

    @Test
    fun `translates a database slug constraint failure`() {
        val failure = constraintFailure("tournaments_slug_unique")
        whenever(tournaments.saveAndFlush(anyTournament()))
            .thenThrow(failure)

        val conflict = assertFailsWith<TournamentSlugConflictException> {
            service.create(request)
        }

        assertSame(failure, conflict.cause)
    }

    @Test
    fun `preserves unrelated persistence failures`() {
        val failure = constraintFailure("another_unique_constraint")
        whenever(tournaments.saveAndFlush(anyTournament()))
            .thenThrow(failure)

        val thrown = assertFailsWith<DataIntegrityViolationException> {
            service.create(request)
        }

        assertSame(failure, thrown)
    }

    // Java Mockito matchers return null; Kotlin needs a non-null placeholder.
    private fun anyTournament(): Tournament =
        any(Tournament::class.java)
            ?: Tournament(slug = "matcher-placeholder", name = "Placeholder")

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
