package com.abeesmall.tournup.model.season

import com.abeesmall.tournup.model.tournament.Tournament

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.FetchType
import jakarta.persistence.ForeignKey
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.Table
import jakarta.persistence.UniqueConstraint
import java.util.UUID
import com.abeesmall.tournup.model.ResourceIdentifiers
import org.hibernate.envers.Audited

@Entity
@Table(
    name = "seasons",
    uniqueConstraints = [
        UniqueConstraint(
            name = "seasons_tournament_name_unique",
            columnNames = ["tournament_id", "name"],
        ),
    ],
)
@Audited
class Season(
    @Id
    val id: UUID = ResourceIdentifiers.uuidV7(),

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
        name = "tournament_id",
        nullable = false,
        updatable = false,
        foreignKey = ForeignKey(name = "seasons_tournament_fk"),
    )
    val tournament: Tournament,

    @Column(nullable = false, length = 255)
    var name: String,

    @Column(nullable = false, length = 255)
    var slug: String,

    @Column(name = "from_year", nullable = false)
    var fromYear: Int,
    @Column(name = "nano_id", nullable = false, unique = true, updatable = false, length = 4)
    val nanoId: String = ResourceIdentifiers.nanoId(),

) {
    val locator: String get() = ResourceIdentifiers.locator(slug, nanoId)
}
