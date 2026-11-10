package com.abeesmall.tournup.service.season

import com.abeesmall.tournup.dto.season.SeasonData
import com.abeesmall.tournup.dto.season.SeasonListPagination
import com.abeesmall.tournup.dto.season.SeasonListResponse
import com.abeesmall.tournup.manifest.season.seasonListManifest
import com.abeesmall.tournup.repository.season.SeasonRepository
import com.abeesmall.tournup.repository.tournament.TournamentRepository
import java.util.UUID
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class SeasonListService(
    private val tournaments: TournamentRepository,
    private val seasons: SeasonRepository,
) {
    @Transactional(readOnly = true)
    fun list(
        tournamentId: UUID?,
        pageable: Pageable,
        canCreate: Boolean = false,
    ): SeasonListResponse {
        if (tournamentId != null && !tournaments.existsById(tournamentId)) {
            throw SeasonTournamentNotFoundException(tournamentId)
        }
        val result = if (tournamentId == null) {
            seasons.findAll(pageable)
        } else {
            seasons.findAllByTournamentId(tournamentId, pageable)
        }

        return SeasonListResponse(
            manifest = seasonListManifest(tournamentId, canCreate),
            items = result.content.map { season ->
                SeasonData(
                    id = season.id,
                    tournamentId = season.tournament.id,
                    name = season.name,
                    slug = season.slug,
                    fromYear = season.fromYear,
                )
            },
            pagination = SeasonListPagination(
                page = result.number,
                size = result.size,
                totalElements = result.totalElements,
                totalPages = result.totalPages,
            ),
        )
    }
}
