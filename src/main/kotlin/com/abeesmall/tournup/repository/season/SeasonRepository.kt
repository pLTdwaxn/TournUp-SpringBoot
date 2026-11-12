package com.abeesmall.tournup.repository.season

import com.abeesmall.tournup.dto.ui.LookupItem
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.data.domain.Pageable

import com.abeesmall.tournup.model.season.Season
import java.util.UUID
import org.springframework.data.domain.Page
import org.springframework.data.jpa.repository.JpaRepository

interface SeasonRepository : JpaRepository<Season, UUID> {
    fun findByNanoId(nanoId: String): Season?


    fun findAllByTournamentId(tournamentId: UUID, pageable: Pageable): Page<Season>

    fun findAllByTournamentIdOrderByFromYearAscNameAsc(tournamentId: UUID): List<Season>

    fun existsByTournamentIdAndName(tournamentId: UUID, name: String): Boolean


    fun existsByTournamentIdAndNameAndIdNot(tournamentId: UUID, name: String, id: UUID): Boolean


    @Query("""
        select new com.abeesmall.tournup.dto.ui.LookupItem(concat(r.slug, '-', r.nanoId), r.name)
        from Season r
        where lower(r.name) like :pattern escape '!'
          and (:afterName is null or r.name > :afterName or (r.name = :afterName and r.nanoId > :afterId))
          and (:tournamentId is null or r.tournament.id = :tournamentId)
        order by r.name, r.nanoId
    """)
    fun lookup(
        @Param("pattern") pattern: String, @Param("afterName") afterName: String?, @Param("afterId") afterId: String?,
        @Param("tournamentId") tournamentId: UUID?,
        pageable: Pageable,
    ): List<LookupItem>
}
