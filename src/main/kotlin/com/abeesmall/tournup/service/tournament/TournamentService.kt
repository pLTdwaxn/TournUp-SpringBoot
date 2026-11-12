package com.abeesmall.tournup.service.tournament

import com.abeesmall.tournup.dto.tournament.TournamentCreateRequest
import com.abeesmall.tournup.dto.tournament.TournamentData
import com.abeesmall.tournup.dto.tournament.TournamentUpdateRequest
import com.abeesmall.tournup.dto.ui.LookupResponse
import com.abeesmall.tournup.model.tournament.Tournament
import com.abeesmall.tournup.repository.tournament.TournamentRepository
import com.abeesmall.tournup.service.LookupQuery
import com.abeesmall.tournup.service.ResourceRecord
import com.abeesmall.tournup.service.parseRecordId
import com.abeesmall.tournup.validation.WriteValidator
import jakarta.validation.Validator
import java.util.UUID
import org.hibernate.exception.ConstraintViolationException
import org.springframework.dao.DataIntegrityViolationException
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class TournamentService(
    private val tournaments: TournamentRepository,
    private val validator: Validator,
) {

    @Transactional(readOnly = true)
    fun lookup(q: String, limit: Int, cursor: String?): LookupResponse {
        val query = LookupQuery.parse("tournament", q, limit, cursor)
        return query.response(tournaments.lookup(
            query.pattern, query.afterName, query.afterId, query.pageable,
        ))
    }

    @Transactional
    fun create(request: TournamentCreateRequest): ResourceRecord<TournamentData> {
        WriteValidator.validate(validator, request)
        if (tournaments.findBySlug(request.slug) != null) {
            throw TournamentSlugConflictException()
        }

        val tournament = save(
            Tournament(
                slug = request.slug,
                name = request.name,
                description = request.description,
                participantType = request.participantType,
                participationMode = request.participationMode,
                handicapMode = request.handicapMode,
            ),
        )

        return ResourceRecord(tournament.toData())
    }

    @Transactional(readOnly = true)
    fun find(identifier: String): ResourceRecord<TournamentData>? {
        val id = parseRecordId(identifier)
        val tournament = if (id != null) {
            tournaments.findById(id).orElse(null)
        } else {
            tournaments.findBySlug(identifier)
        } ?: return null

        return ResourceRecord(tournament.toData())
    }

    @Transactional(readOnly = true)
    fun list(pageable: Pageable): Page<TournamentData> = tournaments.findAll(pageable).map { it.toData() }

    @Transactional(readOnly = true)
    fun editForm(id: UUID): ResourceRecord<TournamentUpdateRequest>? {
        val tournament = tournaments.findById(id).orElse(null)
            ?: return null

        return ResourceRecord(
            item = TournamentUpdateRequest(
                name = tournament.name,
                slug = tournament.slug,
                description = tournament.description,
                participantType = tournament.participantType,
                participationMode = tournament.participationMode,
                handicapMode = tournament.handicapMode,
            ),
        )
    }

    @Transactional
    fun update(
        id: UUID,
        request: TournamentUpdateRequest,
    ): ResourceRecord<TournamentData>? {
        WriteValidator.validate(validator, request)
        val tournament = tournaments.findById(id).orElse(null)
            ?: return null

        val slugOwner = tournaments.findBySlug(request.slug)
        if (slugOwner != null && slugOwner.id != id) {
            throw TournamentSlugConflictException()
        }

        tournament.name = request.name
        tournament.slug = request.slug
        tournament.description = request.description
        tournament.participantType = request.participantType
        tournament.participationMode = request.participationMode
        tournament.handicapMode = request.handicapMode

        val saved = save(tournament)

        return ResourceRecord(saved.toData())
    }

    private fun save(tournament: Tournament): Tournament = try {
        tournaments.saveAndFlush(tournament)
    } catch (failure: DataIntegrityViolationException) {
        val slugConflict = generateSequence<Throwable>(failure) { it.cause }
            .filterIsInstance<ConstraintViolationException>()
            .any { it.constraintName == "tournaments_slug_unique" }
        if (slugConflict) throw TournamentSlugConflictException(failure)
        throw failure
    }
}
