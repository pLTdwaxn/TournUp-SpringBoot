package com.abeesmall.tournup.service.season

import com.abeesmall.tournup.dto.season.SeasonDetailResponse
import com.abeesmall.tournup.dto.season.SeasonUpdateFormResponse
import com.abeesmall.tournup.dto.season.SeasonUpdateRequest
import com.abeesmall.tournup.manifest.season.seasonUpdateManifest
import com.abeesmall.tournup.repository.season.SeasonRepository
import jakarta.validation.ConstraintViolationException
import jakarta.validation.Validator
import java.util.UUID
import org.springframework.dao.DataIntegrityViolationException
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class SeasonUpdateService(
    private val seasons: SeasonRepository,
    private val validator: Validator,
) {
    @Transactional(readOnly = true)
    fun form(id: UUID, tournamentId: UUID? = null): SeasonUpdateFormResponse? {
        val season = seasons.findById(id).orElse(null) ?: return null
        if (tournamentId != null && season.tournament.id != tournamentId) {
            return null
        }
        return SeasonUpdateFormResponse(
            manifest = seasonUpdateManifest(season.id),
            item = SeasonUpdateRequest(season.name, season.slug, season.fromYear, season.tournament.id),
            context = seasonDetailContext(season.tournament),
            included = mapOf(
                "tournament" to listOf(mapOf("id" to season.tournament.id, "name" to season.tournament.name)),
            ),
        )
    }

    @Transactional
    fun update(
        id: UUID,
        request: SeasonUpdateRequest,
        tournamentId: UUID? = null,
    ): SeasonDetailResponse? {
        val violations = validator.validate(request)
        if (violations.isNotEmpty()) {
            throw ConstraintViolationException(violations)
        }
        val season = seasons.findById(id).orElse(null) ?: return null
        val actualTournamentId = season.tournament.id
        if (tournamentId != null && actualTournamentId != tournamentId) {
            return null
        }
        if (request.tournamentId != actualTournamentId) {
            throw SeasonTournamentChangeException()
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

        return seasonDetailResponse(saved, canUpdate = true)
    }
}
