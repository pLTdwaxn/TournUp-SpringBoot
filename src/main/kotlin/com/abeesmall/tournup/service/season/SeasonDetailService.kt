package com.abeesmall.tournup.service.season

import com.abeesmall.tournup.dto.season.SeasonData
import com.abeesmall.tournup.dto.season.SeasonDetailContext
import com.abeesmall.tournup.dto.season.SeasonDetailResponse
import com.abeesmall.tournup.dto.season.SeasonTournamentContext
import com.abeesmall.tournup.manifest.season.seasonDetailManifest
import com.abeesmall.tournup.model.season.Season
import com.abeesmall.tournup.model.tournament.Tournament
import com.abeesmall.tournup.repository.season.SeasonRepository
import com.abeesmall.tournup.repository.tournament.TournamentRepository
import com.abeesmall.tournup.service.RecordContextRequiredException
import com.abeesmall.tournup.service.parseRecordId
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class SeasonDetailService(
    private val seasons: SeasonRepository,
    private val tournaments: TournamentRepository,
) {
    @Transactional(readOnly = true)
    fun find(
        identifier: String,
        tournamentIdentifier: String? = null,
        canUpdate: Boolean = false,
    ): SeasonDetailResponse? {
        val id = parseRecordId(identifier)
        if (id == null && tournamentIdentifier == null) {
            throw RecordContextRequiredException("Tournament")
        }
        val tournamentId = tournamentIdentifier?.let { parent ->
            parseRecordId(parent) ?: tournaments.findBySlug(parent)?.id ?: return null
        }
        val season = if (id != null) {
            seasons.findById(id).orElse(null)
        } else {
            seasons.findByTournamentIdAndSlug(requireNotNull(tournamentId), identifier)
        } ?: return null
        val tournament = season.tournament
        if (tournamentId != null && tournament.id != tournamentId) {
            return null
        }

        return seasonDetailResponse(season, canUpdate)
    }
}

internal fun seasonDetailResponse(season: Season, canUpdate: Boolean): SeasonDetailResponse =
    SeasonDetailResponse(
        manifest = seasonDetailManifest(
            title = season.name,
            editHref = if (canUpdate) "/api/seasons/${season.id}/edit/" else null,
        ),
        item = SeasonData(
            id = season.id,
            tournamentId = season.tournament.id,
            name = season.name,
            slug = season.slug,
            fromYear = season.fromYear,
        ),
        context = seasonDetailContext(season.tournament),
    )

internal fun seasonDetailContext(tournament: Tournament): SeasonDetailContext =
    SeasonDetailContext(
        tournament = SeasonTournamentContext(
            id = tournament.id,
            name = tournament.name,
            href = "/api/tournaments/${tournament.id}/",
        ),
        collectionHref = "/api/tournaments/${tournament.id}/seasons/",
    )
