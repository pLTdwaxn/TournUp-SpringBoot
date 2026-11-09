package com.abeesmall.tournup.service.tournament

import com.abeesmall.tournup.dto.tournament.TournamentData
import com.abeesmall.tournup.dto.tournament.TournamentDetailResponse
import com.abeesmall.tournup.dto.tournament.TournamentUpdateFormResponse
import com.abeesmall.tournup.dto.tournament.TournamentUpdateRequest
import com.abeesmall.tournup.manifest.tournament.tournamentDetailManifest
import com.abeesmall.tournup.manifest.tournament.tournamentUpdateManifest
import com.abeesmall.tournup.repository.tournament.TournamentRepository
import java.util.UUID
import org.hibernate.exception.ConstraintViolationException
import org.springframework.dao.DataIntegrityViolationException
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class TournamentUpdateService(
    private val tournaments: TournamentRepository,
) {
    @Transactional(readOnly = true)
    fun form(id: UUID): TournamentUpdateFormResponse? {
        val tournament = tournaments.findById(id).orElse(null)
            ?: return null

        return TournamentUpdateFormResponse(
            manifest = tournamentUpdateManifest(tournament.id),
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
    ): TournamentDetailResponse? {
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

        val saved = try {
            tournaments.saveAndFlush(tournament)
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
                title = saved.name,
                editHref = "/api/tournaments/${saved.id}/edit/",
            ),
            item = TournamentData(
                id = saved.id,
                slug = saved.slug,
                name = saved.name,
                description = saved.description,
                participantType = saved.participantType,
                participationMode = saved.participationMode,
                handicapMode = saved.handicapMode,
            ),
        )
    }
}
