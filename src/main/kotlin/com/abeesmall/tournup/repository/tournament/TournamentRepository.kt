package com.abeesmall.tournup.repository.tournament

import com.abeesmall.tournup.model.tournament.Tournament
import java.util.UUID
import org.springframework.data.jpa.repository.JpaRepository

interface TournamentRepository : JpaRepository<Tournament, UUID> {

    fun findBySlug(slug: String): Tournament?
}
