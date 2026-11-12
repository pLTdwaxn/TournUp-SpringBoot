package com.abeesmall.tournup.service.season

import com.abeesmall.tournup.dto.season.SeasonData
import com.abeesmall.tournup.model.season.Season


internal fun Season.toData(): SeasonData = SeasonData(
    internalId = id,
    nanoId = nanoId,
    tournamentId = com.abeesmall.tournup.model.ResourceIdentifiers.locator(tournament.slug, tournament.nanoId),
    name = name,
    slug = slug,
    fromYear = fromYear,
)
