package com.abeesmall.tournup.repository.tournament

import com.abeesmall.tournup.dto.ui.LookupItem
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.data.domain.Pageable

import com.abeesmall.tournup.model.tournament.Tournament
import java.util.UUID
import org.springframework.data.jpa.repository.JpaRepository

interface TournamentRepository : JpaRepository<Tournament, UUID> {
    fun findByNanoId(nanoId: String): Tournament?


    fun findBySlug(slug: String): Tournament?

    @Query("""
        select new com.abeesmall.tournup.dto.ui.LookupItem(concat(r.slug, '-', r.nanoId), r.name)
        from Tournament r
        where lower(r.name) like :pattern escape '!'
          and (:afterName is null or r.name > :afterName or (r.name = :afterName and r.nanoId > :afterId))
        order by r.name, r.nanoId
    """)
    fun lookup(
        @Param("pattern") pattern: String, @Param("afterName") afterName: String?, @Param("afterId") afterId: String?,
        pageable: Pageable,
    ): List<LookupItem>
}
