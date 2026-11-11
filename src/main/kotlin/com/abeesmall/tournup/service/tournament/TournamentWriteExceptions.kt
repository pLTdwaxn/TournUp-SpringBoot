package com.abeesmall.tournup.service.tournament




class TournamentSlugConflictException(
    cause: Throwable? = null,
) : RuntimeException(
    "A tournament with this slug already exists",
    cause,
)

