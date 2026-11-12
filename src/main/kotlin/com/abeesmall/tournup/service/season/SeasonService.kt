package com.abeesmall.tournup.service.season

import com.abeesmall.tournup.dto.season.SeasonData
import com.abeesmall.tournup.dto.season.SeasonNamingContext
import com.abeesmall.tournup.dto.season.SeasonWriteRequest
import com.abeesmall.tournup.dto.ui.LookupResponse
import com.abeesmall.tournup.model.season.Season
import com.abeesmall.tournup.model.tournament.Tournament
import com.abeesmall.tournup.repository.season.SeasonRepository
import com.abeesmall.tournup.repository.tournament.TournamentRepository
import com.abeesmall.tournup.resource.season.seasonCreateContract
import com.abeesmall.tournup.resource.season.seasonUpdateContract
import com.abeesmall.tournup.service.LookupQuery
import com.abeesmall.tournup.service.RecordContextRequiredException
import com.abeesmall.tournup.service.ResourceRecord
import com.abeesmall.tournup.service.parseRecordId
import com.abeesmall.tournup.service.tournament.includedReference
import com.abeesmall.tournup.validation.WriteValidator
import jakarta.validation.Validator
import java.util.UUID
import org.springframework.dao.DataIntegrityViolationException
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class SeasonService(
    private val tournaments: TournamentRepository,
    private val seasons: SeasonRepository,
    private val validator: Validator,
) {

    @Transactional(readOnly = true)
    fun lookup(q: String, limit: Int, cursor: String?, tournamentId: UUID? = null): LookupResponse {
        if (tournamentId != null) requireTournament(tournamentId)
        val query = LookupQuery.parse("season:" + (tournamentId?.toString() ?: "all"), q, limit, cursor)
        return query.response(seasons.lookup(
            query.pattern, query.afterName, query.afterId, tournamentId, query.pageable,
        ))
    }

    @Transactional(readOnly = true)
    fun createForm(tournamentId: UUID?): ResourceRecord<SeasonWriteRequest> {
        val initial = requireNotNull(seasonCreateContract.initialValues).invoke()
        if (tournamentId == null) return ResourceRecord(initial)
        val tournament = requireTournament(tournamentId)
        return ResourceRecord(initial.copy(tournamentId = tournament.id), tournament.includedReference())
    }

    @Transactional(readOnly = true)
    fun namingContext(tournamentId: UUID): SeasonNamingContext {
        requireTournament(tournamentId)
        val existing = seasons.findAllByTournamentIdOrderByFromYearAscNameAsc(tournamentId)

        return SeasonNamingContext(
            tournamentId = tournamentId,
            existingNamesByYear = existing.groupBy(
                keySelector = { it.fromYear },
                valueTransform = { it.name },
            ),
        )
    }

    @Transactional
    fun create(request: SeasonWriteRequest): ResourceRecord<SeasonData> {
        WriteValidator.validate(validator, request)

        val tournamentId = requireNotNull(request.tournamentId)
        val tournament = requireTournament(tournamentId)
        if (seasons.existsByTournamentIdAndName(tournamentId, request.name)) {
            throw SeasonFieldConflictException("name")
        }
        if (seasons.existsByTournamentIdAndSlug(tournamentId, request.slug)) {
            throw SeasonFieldConflictException("slug")
        }

        val season = try {
            seasons.saveAndFlush(
                Season(
                    tournament = tournament,
                    name = request.name,
                    slug = request.slug,
                    fromYear = requireNotNull(request.fromYear),
                ),
            )
        } catch (failure: DataIntegrityViolationException) {
            throw seasonFieldConflict(failure) ?: failure
        }

        return seasonRecord(season)
    }

    private fun requireTournament(tournamentId: UUID): Tournament =
        tournaments.findById(tournamentId).orElseThrow {
            SeasonTournamentNotFoundException(tournamentId)
        }

    @Transactional(readOnly = true)
    fun find(
        identifier: String,
        tournamentIdentifier: String? = null,
    ): ResourceRecord<SeasonData>? {
        val id = parseRecordId(identifier)
        if (id == null && tournamentIdentifier == null) {
            throw RecordContextRequiredException("Tournament")
        }
        val tournamentId = tournamentIdentifier?.let { parent ->
            parseRecordId(parent) ?: tournaments.findBySlug(parent)?.id ?: return null
        }
        val season = if (id != null) {
            seasons.findById(id).orElse(null)
        } else {
            seasons.findByTournamentIdAndSlug(requireNotNull(tournamentId), identifier)
        } ?: return null
        val tournament = season.tournament
        if (tournamentId != null && tournament.id != tournamentId) {
            return null
        }

        return seasonRecord(season)
    }

    @Transactional(readOnly = true)
    fun list(tournamentId: UUID?, pageable: Pageable): Page<SeasonData> {
        if (tournamentId != null && !tournaments.existsById(tournamentId)) {
            throw SeasonTournamentNotFoundException(tournamentId)
        }
        val result = if (tournamentId == null) seasons.findAll(pageable)
            else seasons.findAllByTournamentId(tournamentId, pageable)
        return result.map { it.toData() }
    }

    @Transactional(readOnly = true)
    fun editForm(id: UUID, tournamentId: UUID? = null): ResourceRecord<SeasonWriteRequest>? {
        val season = seasons.findById(id).orElse(null) ?: return null
        if (tournamentId != null && season.tournament.id != tournamentId) {
            return null
        }
        return ResourceRecord(
            item = SeasonWriteRequest(season.name, season.slug, season.fromYear, season.tournament.id),
            included = season.tournament.includedReference(),
        )
    }

    @Transactional
    fun update(
        id: UUID,
        request: SeasonWriteRequest,
        tournamentId: UUID? = null,
    ): ResourceRecord<SeasonData>? {
        WriteValidator.validate(validator, request)
        val season = seasons.findById(id).orElse(null) ?: return null
        val actualTournamentId = season.tournament.id
        if (tournamentId != null && actualTournamentId != tournamentId) {
            return null
        }
        seasonUpdateContract.requireUnchanged(request, mapOf("tournamentId" to actualTournamentId)) {
            SeasonTournamentChangeException()
        }

        if (seasons.existsByTournamentIdAndNameAndIdNot(actualTournamentId, request.name, id)) {
            throw SeasonFieldConflictException("name")
        }
        if (seasons.existsByTournamentIdAndSlugAndIdNot(actualTournamentId, request.slug, id)) {
            throw SeasonFieldConflictException("slug")
        }

        season.name = request.name
        season.slug = request.slug
        season.fromYear = requireNotNull(request.fromYear)
        val saved = try {
            seasons.saveAndFlush(season)
        } catch (failure: DataIntegrityViolationException) {
            throw seasonFieldConflict(failure) ?: failure
        }

        return seasonRecord(saved)
    }
}
private fun seasonRecord(season: Season): ResourceRecord<SeasonData> =
    ResourceRecord(season.toData(), season.tournament.includedReference())
