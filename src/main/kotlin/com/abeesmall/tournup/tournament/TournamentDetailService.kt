package com.abeesmall.tournup.tournament

import java.util.UUID
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class TournamentDetailService(
    private val tournaments: TournamentRepository,
) {
    @Transactional(readOnly = true)
    fun find(id: UUID): TournamentDetailResponse? {
        val tournament = tournaments.findById(id).orElse(null)
            ?: return null

        return TournamentDetailResponse(
            manifest = tournamentDetailManifest(tournament.name),
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
