package com.abeesmall.tournup.service.tournament

import com.abeesmall.tournup.dto.tournament.TournamentData
import com.abeesmall.tournup.dto.tournament.TournamentDetailResponse
import com.abeesmall.tournup.manifest.tournament.tournamentDetailManifest
import com.abeesmall.tournup.repository.tournament.TournamentRepository
import java.util.UUID
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class TournamentDetailService(
    private val tournaments: TournamentRepository,
) {
    @Transactional(readOnly = true)
    fun find(
        id: UUID,
        canUpdate: Boolean = false,
    ): TournamentDetailResponse? {
        val tournament = tournaments.findById(id).orElse(null)
            ?: return null

        return TournamentDetailResponse(
            manifest = tournamentDetailManifest(
                title = tournament.name,
                editHref = if (canUpdate) {
                    "/api/tournaments/${tournament.id}/edit/"
                } else {
                    null
                },
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
