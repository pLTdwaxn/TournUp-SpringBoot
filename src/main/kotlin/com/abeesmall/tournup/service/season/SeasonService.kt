package com.abeesmall.tournup.service.season

import com.abeesmall.tournup.dto.season.SeasonData
import com.abeesmall.tournup.dto.season.SeasonNamingContext
import com.abeesmall.tournup.dto.season.SeasonWriteRequest
import com.abeesmall.tournup.dto.ui.LookupResponse
import com.abeesmall.tournup.dto.season.seasonCreateContract
import com.abeesmall.tournup.dto.season.seasonUpdateContract
import com.abeesmall.tournup.model.season.Season
import com.abeesmall.tournup.model.tournament.Tournament
import com.abeesmall.tournup.repository.season.SeasonRepository
import com.abeesmall.tournup.repository.tournament.TournamentRepository
import com.abeesmall.tournup.service.nanoIdConflict
import com.abeesmall.tournup.service.LookupQuery
import com.abeesmall.tournup.service.ResourceRecord
import com.abeesmall.tournup.model.ResourceIdentifiers
import com.abeesmall.tournup.service.tournament.includedReference
import com.abeesmall.tournup.validation.WriteValidator
import jakarta.validation.Validator
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
    fun lookup(q: String, limit: Int, cursor: String?, tournamentId: String? = null): LookupResponse {
        val parent = tournamentId?.let(::requireTournament)
        val query = LookupQuery.parse("season:" + (parent?.nanoId ?: "all"), q, limit, cursor)
        return query.response(seasons.lookup(
            query.pattern, query.afterName, query.afterId, parent?.id, query.pageable,
        ))
    }

    @Transactional(readOnly = true)
    fun createForm(tournamentId: String?): ResourceRecord<SeasonWriteRequest> {
        val initial = requireNotNull(seasonCreateContract.initialValues).invoke()
        if (tournamentId == null) return ResourceRecord(initial)
        val tournament = requireTournament(tournamentId)
        return ResourceRecord(initial.copy(tournamentId = tournament.locator), tournament.includedReference())
    }

    @Transactional(readOnly = true)
    fun namingContext(tournamentId: String): SeasonNamingContext {
        val tournament = requireTournament(tournamentId)
        val existing = seasons.findAllByTournamentIdOrderByFromYearAscNameAsc(tournament.id)

        return SeasonNamingContext(
            tournamentId = tournament.locator,
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
        if (seasons.existsByTournamentIdAndName(tournament.id, request.name)) {
            throw SeasonFieldConflictException("name")
        }

        val season = try {
            seasons.saveAndFlush(
                Season(
                    tournament = tournament,
                    name = request.name,
                    slug = request.slug,
                    fromYear = requireNotNull(request.fromYear),
                    nanoId = request.nanoId ?: ResourceIdentifiers.nanoId(),
                ),
            )
        } catch (failure: DataIntegrityViolationException) {
            throw nanoIdConflict(failure) ?: seasonFieldConflict(failure) ?: failure
        }

        return seasonRecord(season)
    }

    private fun requireTournament(tournamentId: String): Tournament =
        ResourceIdentifiers.nanoId(tournamentId)?.let(tournaments::findByNanoId)
            ?: throw SeasonTournamentNotFoundException(tournamentId)

    @Transactional(readOnly = true)
    fun find(identifier: String, tournamentIdentifier: String? = null): ResourceRecord<SeasonData>? {
        val season = ResourceIdentifiers.nanoId(identifier)?.let(seasons::findByNanoId) ?: return null
        if (tournamentIdentifier != null && requireTournament(tournamentIdentifier).id != season.tournament.id) return null
        return seasonRecord(season)
    }

    @Transactional(readOnly = true)
    fun list(tournamentId: String?, pageable: Pageable): Page<SeasonData> {
        val parent = tournamentId?.let(::requireTournament)
        val result = if (parent == null) seasons.findAll(pageable)
            else seasons.findAllByTournamentId(parent.id, pageable)
        return result.map { it.toData() }
    }

    @Transactional(readOnly = true)
    fun editForm(id: String, tournamentId: String? = null): ResourceRecord<SeasonWriteRequest>? {
        val season = ResourceIdentifiers.nanoId(id)?.let(seasons::findByNanoId) ?: return null
        if (tournamentId != null && season.tournament.id != requireTournament(tournamentId).id) {
            return null
        }
        return ResourceRecord(
            item = SeasonWriteRequest(season.name, season.slug, season.fromYear, season.tournament.locator, season.nanoId),
            included = season.tournament.includedReference(),
        )
    }

    @Transactional
    fun update(
        id: String,
        request: SeasonWriteRequest,
        tournamentId: String? = null,
    ): ResourceRecord<SeasonData>? {
        WriteValidator.validate(validator, request)
        val season = ResourceIdentifiers.nanoId(id)?.let(seasons::findByNanoId) ?: return null
        val actualTournamentId = season.tournament.id
        if (tournamentId != null && actualTournamentId != requireTournament(tournamentId).id) {
            return null
        }
        seasonUpdateContract.requireUnchanged(
            request.copy(tournamentId = ResourceIdentifiers.nanoId(requireNotNull(request.tournamentId)),
                nanoId = request.nanoId ?: season.nanoId),
            mapOf("tournamentId" to season.tournament.nanoId, "nanoId" to season.nanoId),
        ) { field ->
            if (field == "nanoId") org.springframework.web.server.ResponseStatusException(
                org.springframework.http.HttpStatus.BAD_REQUEST, "NanoID cannot be changed after creation",
            ) else SeasonTournamentChangeException()
        }

        if (seasons.existsByTournamentIdAndNameAndIdNot(actualTournamentId, request.name, season.id)) {
            throw SeasonFieldConflictException("name")
        }

        season.name = request.name
        season.slug = request.slug
        season.fromYear = requireNotNull(request.fromYear)
        val saved = try {
            seasons.saveAndFlush(season)
        } catch (failure: DataIntegrityViolationException) {
            throw nanoIdConflict(failure) ?: seasonFieldConflict(failure) ?: failure
        }

        return seasonRecord(saved)
    }
}
private fun seasonRecord(season: Season): ResourceRecord<SeasonData> =
    ResourceRecord(season.toData(), season.tournament.includedReference())
