package com.abeesmall.tournup.tournament

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.Id
import jakarta.persistence.PreUpdate
import jakarta.persistence.Table
import java.time.Instant
import java.util.UUID

enum class HandicapMode {
    NO_HANDICAP,
    RESET_EACH_SEASON,
    CARRY_FORWARD,
}

enum class ParticipationMode {
    WALK_IN,
    REGISTRATION,
}

enum class ParticipantType {
    PLAYER,
    TEAM,
}

@Entity
@Table(name = "tournaments")
class Tournament(
    @Id
    val id: UUID = UUID.randomUUID(),

    @Column(nullable = false, unique = true, length = 255)
    var slug: String,

    @Column(nullable = false, length = 255)
    var name: String,

    @Column(columnDefinition = "text")
    var description: String? = null,

    @Enumerated(EnumType.STRING)
    @Column(name = "handicap_mode", nullable = false, length = 20)
    var handicapMode: HandicapMode = HandicapMode.NO_HANDICAP,

    @Enumerated(EnumType.STRING)
    @Column(name = "participation_mode", nullable = false, length = 20)
    var participationMode: ParticipationMode = ParticipationMode.WALK_IN,

    @Enumerated(EnumType.STRING)
    @Column(name = "participant_type", nullable = false, length = 20)
    var participantType: ParticipantType = ParticipantType.PLAYER,

    @Column(name = "created_at", nullable = false, updatable = false)
    val createdAt: Instant = Instant.now(),

    @Column(name = "updated_at", nullable = false)
    var updatedAt: Instant = Instant.now(),
) {
    @PreUpdate
    fun markUpdated() {
        updatedAt = Instant.now()
    }
}
