package com.abeesmall.tournup.service.season

import com.abeesmall.tournup.dto.season.SeasonData
import com.abeesmall.tournup.dto.season.SeasonDetailContext
import com.abeesmall.tournup.dto.season.SeasonDetailResponse
import com.abeesmall.tournup.dto.season.SeasonTournamentContext
import com.abeesmall.tournup.manifest.season.seasonDetailManifest
import com.abeesmall.tournup.repository.season.SeasonRepository
import java.util.UUID
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class SeasonDetailService(
    private val seasons: SeasonRepository,
) {
    @Transactional(readOnly = true)
    fun find(id: UUID): SeasonDetailResponse? {
        val season = seasons.findById(id).orElse(null) ?: return null
        val tournament = season.tournament

        return SeasonDetailResponse(
            manifest = seasonDetailManifest(season.name),
            item = SeasonData(
                id = season.id,
                tournamentId = tournament.id,
                name = season.name,
                slug = season.slug,
                fromYear = season.fromYear,
            ),
            context = SeasonDetailContext(
                tournament = SeasonTournamentContext(
                    id = tournament.id,
                    name = tournament.name,
                    href = "/api/tournaments/${tournament.id}/",
                ),
                collectionHref = "/api/tournaments/${tournament.id}/seasons/",
            ),
        )
    }
}
