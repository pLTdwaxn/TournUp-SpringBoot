package com.abeesmall.tournup.tournament

import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class TournamentListService(
    private val tournaments: TournamentRepository,
) {
    @Transactional(readOnly = true)
    fun list(pageable: Pageable): TournamentListResponse {
        val result = tournaments.findAll(pageable)

        return TournamentListResponse(
            manifest = tournamentListManifest(),
            items = result.content.map { tournament ->
                TournamentData(
                    id = tournament.id,
                    slug = tournament.slug,
                    name = tournament.name,
                    description = tournament.description,
                    participantType = tournament.participantType,
                    participationMode = tournament.participationMode,
                    handicapMode = tournament.handicapMode,
                )
            },
            pagination = TournamentListPagination(
                page = result.number,
                size = result.size,
                totalElements = result.totalElements,
                totalPages = result.totalPages,
            ),
        )
    }
}
