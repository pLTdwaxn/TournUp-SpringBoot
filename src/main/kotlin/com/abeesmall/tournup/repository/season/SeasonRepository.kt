package com.abeesmall.tournup.repository.season

import com.abeesmall.tournup.model.season.Season
import java.util.UUID
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository

interface SeasonRepository : JpaRepository<Season, UUID> {
    fun findByTournamentIdAndSlug(tournamentId: UUID, slug: String): Season?

    fun findAllByTournamentId(tournamentId: UUID, pageable: Pageable): Page<Season>

    fun findAllByTournamentIdOrderByFromYearAscNameAsc(tournamentId: UUID): List<Season>

    fun existsByTournamentIdAndName(tournamentId: UUID, name: String): Boolean

    fun existsByTournamentIdAndSlug(tournamentId: UUID, slug: String): Boolean

    fun existsByTournamentIdAndNameAndIdNot(tournamentId: UUID, name: String, id: UUID): Boolean

    fun existsByTournamentIdAndSlugAndIdNot(tournamentId: UUID, slug: String, id: UUID): Boolean
}
