package com.abeesmall.tournup.repository.tournament

import com.abeesmall.tournup.dto.ui.LookupItem
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.data.domain.Pageable

import com.abeesmall.tournup.model.tournament.Tournament
import java.util.UUID
import org.springframework.data.jpa.repository.JpaRepository

interface TournamentRepository : JpaRepository<Tournament, UUID> {

    fun findBySlug(slug: String): Tournament?

    @Query("""
        select new com.abeesmall.tournup.dto.ui.LookupItem(r.id, r.name)
        from Tournament r
        where lower(r.name) like :pattern escape '!'
          and (:afterName is null or r.name > :afterName or (r.name = :afterName and r.id > :afterId))
        order by r.name, r.id
    """)
    fun lookup(
        @Param("pattern") pattern: String, @Param("afterName") afterName: String?, @Param("afterId") afterId: UUID?,
        pageable: Pageable,
    ): List<LookupItem>
}
