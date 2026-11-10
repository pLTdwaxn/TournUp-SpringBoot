package com.abeesmall.tournup.service.season

import com.abeesmall.tournup.dto.season.SeasonCreateContext
import com.abeesmall.tournup.dto.season.SeasonCreateFormResponse
import com.abeesmall.tournup.dto.season.SeasonCreateRequest
import com.abeesmall.tournup.dto.season.SeasonData
import com.abeesmall.tournup.manifest.season.seasonCreateManifest
import com.abeesmall.tournup.model.season.Season
import com.abeesmall.tournup.model.tournament.Tournament
import com.abeesmall.tournup.repository.season.SeasonRepository
import com.abeesmall.tournup.repository.tournament.TournamentRepository
import jakarta.validation.ConstraintViolationException
import jakarta.validation.Validator
import java.util.UUID
import org.hibernate.exception.ConstraintViolationException as DatabaseConstraintViolationException
import org.springframework.dao.DataIntegrityViolationException
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

class SeasonTournamentNotFoundException(tournamentId: UUID) :
    RuntimeException("Tournament not found: $tournamentId")

class SeasonFieldConflictException(
    val field: String,
    cause: Throwable? = null,
) : RuntimeException("A season with this $field already exists in this tournament", cause)

@Service
class SeasonCreateService(
    private val tournaments: TournamentRepository,
    private val seasons: SeasonRepository,
    private val validator: Validator,
) {
    @Transactional(readOnly = true)
    fun form(tournamentId: UUID): SeasonCreateFormResponse {
        requireTournament(tournamentId)
        val existing = seasons.findAllByTournamentIdOrderByFromYearAscNameAsc(tournamentId)

        return SeasonCreateFormResponse(
            manifest = seasonCreateManifest(tournamentId),
            context = SeasonCreateContext(
                tournamentId = tournamentId,
                existingNamesByYear = existing.groupBy(
                    keySelector = { it.fromYear },
                    valueTransform = { it.name },
                ),
            ),
        )
    }

    @Transactional
    fun create(tournamentId: UUID, request: SeasonCreateRequest): SeasonData {
        val violations = validator.validate(request)
        if (violations.isNotEmpty()) {
            throw ConstraintViolationException(violations)
        }

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
            val field = generateSequence<Throwable>(failure) { it.cause }
                .filterIsInstance<DatabaseConstraintViolationException>()
                .mapNotNull {
                    when (it.constraintName) {
                        "seasons_tournament_name_unique" -> "name"
                        "seasons_tournament_slug_unique" -> "slug"
                        else -> null
                    }
                }
                .firstOrNull()

            if (field != null) {
                throw SeasonFieldConflictException(field, failure)
            }
            throw failure
        }

        return SeasonData(
            id = season.id,
            tournamentId = tournamentId,
            name = season.name,
            slug = season.slug,
            fromYear = season.fromYear,
        )
    }

    private fun requireTournament(tournamentId: UUID): Tournament =
        tournaments.findById(tournamentId).orElseThrow {
            SeasonTournamentNotFoundException(tournamentId)
        }
}
