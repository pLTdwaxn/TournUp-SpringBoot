package com.abeesmall.tournup.service.season

import java.util.UUID
import org.hibernate.exception.ConstraintViolationException
import org.springframework.dao.DataIntegrityViolationException

class SeasonTournamentNotFoundException(tournamentId: UUID) :
    RuntimeException("Tournament not found: $tournamentId")

class SeasonFieldConflictException(
    val field: String,
    cause: Throwable? = null,
) : RuntimeException("A season with this $field already exists in this tournament", cause)

internal fun seasonFieldConflict(failure: DataIntegrityViolationException): SeasonFieldConflictException? {
    val field = generateSequence<Throwable>(failure) { it.cause }
        .filterIsInstance<ConstraintViolationException>()
        .mapNotNull {
            when (it.constraintName) {
                "seasons_tournament_name_unique" -> "name"
                "seasons_tournament_slug_unique" -> "slug"
                else -> null
            }
        }
        .firstOrNull()

    return field?.let { SeasonFieldConflictException(it, failure) }
}
