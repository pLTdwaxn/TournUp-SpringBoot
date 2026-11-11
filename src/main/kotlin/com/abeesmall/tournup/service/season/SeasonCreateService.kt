package com.abeesmall.tournup.service.season

import com.abeesmall.tournup.dto.season.SeasonNamingContext
import com.abeesmall.tournup.dto.season.SeasonCreateFormResponse
import com.abeesmall.tournup.dto.season.SeasonCreateRequest
import com.abeesmall.tournup.dto.season.SeasonDetailResponse
import com.abeesmall.tournup.manifest.season.seasonCreateManifest
import com.abeesmall.tournup.model.season.Season
import com.abeesmall.tournup.model.tournament.Tournament
import com.abeesmall.tournup.repository.season.SeasonRepository
import com.abeesmall.tournup.repository.tournament.TournamentRepository
import com.abeesmall.tournup.ui.UiApplication
import jakarta.validation.ConstraintViolationException
import jakarta.validation.Validator
import java.util.UUID
import org.springframework.dao.DataIntegrityViolationException
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class SeasonCreateService(
    private val tournaments: TournamentRepository,
    private val seasons: SeasonRepository,
    private val validator: Validator,
) {
    @Transactional(readOnly = true)
    fun form(application: UiApplication, tournamentId: UUID?): SeasonCreateFormResponse {
        val manifest = seasonCreateManifest(application)
        if (tournamentId == null) return SeasonCreateFormResponse(manifest)
        val tournament = requireTournament(tournamentId)
        return SeasonCreateFormResponse(
            manifest = manifest,
            item = SeasonCreateRequest("", "", null, tournament.id),
            included = mapOf(
                "tournament" to listOf(mapOf("id" to tournament.id, "name" to tournament.name)),
            ),
        )
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
    fun create(request: SeasonCreateRequest): SeasonDetailResponse {
        val violations = validator.validate(request)
        if (violations.isNotEmpty()) {
            throw ConstraintViolationException(violations)
        }

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

        return seasonDetailResponse(season, canUpdate = true)
    }

    private fun requireTournament(tournamentId: UUID): Tournament =
        tournaments.findById(tournamentId).orElseThrow {
            SeasonTournamentNotFoundException(tournamentId)
        }
}
