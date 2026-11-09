package com.abeesmall.tournup.service.tournament

import com.abeesmall.tournup.dto.tournament.TournamentData
import com.abeesmall.tournup.dto.tournament.TournamentListPagination
import com.abeesmall.tournup.dto.tournament.TournamentListResponse
import com.abeesmall.tournup.manifest.tournament.tournamentListManifest
import com.abeesmall.tournup.repository.tournament.TournamentRepository
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class TournamentListService(
    private val tournaments: TournamentRepository,
) {
    @Transactional(readOnly = true)
    fun list(pageable: Pageable, canCreate: Boolean = false): TournamentListResponse {
        val result = tournaments.findAll(pageable)

        return TournamentListResponse(
            manifest = tournamentListManifest(canCreate),
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
