package com.abeesmall.tournup.service.season

import com.abeesmall.tournup.dto.season.SeasonData
import com.abeesmall.tournup.model.season.Season


internal fun Season.toData(): SeasonData = SeasonData(
    id = id,
    tournamentId = tournament.id,
    name = name,
    slug = slug,
    fromYear = fromYear,
)
