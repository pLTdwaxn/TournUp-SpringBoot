package com.abeesmall.tournup.tournament

import org.springframework.data.jpa.repository.JpaRepository
import java.util.UUID

interface TournamentRepository : JpaRepository<Tournament, UUID> {

    fun findBySlug(slug: String): Tournament?
}
