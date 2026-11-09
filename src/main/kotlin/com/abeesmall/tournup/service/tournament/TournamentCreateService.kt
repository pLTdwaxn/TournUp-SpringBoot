package com.abeesmall.tournup.service.tournament

import com.abeesmall.tournup.dto.tournament.TournamentCreateRequest
import com.abeesmall.tournup.dto.tournament.TournamentData
import com.abeesmall.tournup.dto.tournament.TournamentDetailResponse
import com.abeesmall.tournup.manifest.tournament.tournamentDetailManifest
import com.abeesmall.tournup.model.tournament.Tournament
import com.abeesmall.tournup.repository.tournament.TournamentRepository
import org.hibernate.exception.ConstraintViolationException
import org.springframework.dao.DataIntegrityViolationException
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

class TournamentSlugConflictException(
    cause: Throwable? = null,
) : RuntimeException(
    "A tournament with this slug already exists",
    cause,
)

@Service
class TournamentCreateService(
    private val tournaments: TournamentRepository,
) {
    @Transactional
    fun create(request: TournamentCreateRequest): TournamentDetailResponse {
        if (tournaments.findBySlug(request.slug) != null) {
            throw TournamentSlugConflictException()
        }

        val tournament = try {
            tournaments.saveAndFlush(
                Tournament(
                    slug = request.slug,
                    name = request.name,
                    description = request.description,
                    participantType = request.participantType,
                    participationMode = request.participationMode,
                    handicapMode = request.handicapMode,
                ),
            )
        } catch (failure: DataIntegrityViolationException) {
            val slugConflict = generateSequence<Throwable>(failure) { it.cause }
                .filterIsInstance<ConstraintViolationException>()
                .any { it.constraintName == "tournaments_slug_unique" }

            if (slugConflict) {
                throw TournamentSlugConflictException(failure)
            }
            throw failure
        }

        return TournamentDetailResponse(
            manifest = tournamentDetailManifest(
                title = tournament.name,
                editHref = "/api/tournaments/${tournament.id}/edit/",
            ),
            item = TournamentData(
                id = tournament.id,
                slug = tournament.slug,
                name = tournament.name,
                description = tournament.description,
                participantType = tournament.participantType,
                participationMode = tournament.participationMode,
                handicapMode = tournament.handicapMode,
            ),
        )
    }
}
