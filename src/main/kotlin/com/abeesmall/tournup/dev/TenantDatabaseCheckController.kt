package com.abeesmall.tournup.dev

import com.abeesmall.tournup.tournament.TournamentRepository
import jakarta.persistence.EntityManager
import jakarta.persistence.PersistenceContext
import java.util.UUID
import org.springframework.context.annotation.Profile
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@Profile("dev")
class TenantDatabaseCheckController(
    private val tournaments: TournamentRepository,
) {

    @PersistenceContext
    private lateinit var entityManager: EntityManager

    @GetMapping("/api/dev/database/")
    @Transactional(readOnly = true)
    fun database(): Map<String, String?> {
        val result = entityManager.createNativeQuery(
            "select current_database(), current_user",
        ).singleResult as Array<*>

        val tournament = tournaments.findById(
            UUID.fromString("00000000-0000-0000-0000-000000000100"),
        ).orElse(null)

        return mapOf(
            "database" to result[0].toString(),
            "user" to result[1].toString(),
            "tournamentId" to tournament?.id?.toString(),
            "tournamentName" to tournament?.name,
        )
    }
}
