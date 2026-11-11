package com.abeesmall.tournup.model.season

import com.abeesmall.tournup.model.tournament.Tournament
import com.abeesmall.tournup.ui.resource.UiResource

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
import org.hibernate.envers.Audited

@Entity
@UiResource(key = "season", pluralName = "seasons")
@Table(
    name = "seasons",
    uniqueConstraints = [
        UniqueConstraint(
            name = "seasons_tournament_name_unique",
            columnNames = ["tournament_id", "name"],
        ),
        UniqueConstraint(
            name = "seasons_tournament_slug_unique",
            columnNames = ["tournament_id", "slug"],
        ),
    ],
)
@Audited
class Season(
    @Id
    val id: UUID = UUID.randomUUID(),

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
        name = "tournament_id",
        nullable = false,
        foreignKey = ForeignKey(name = "seasons_tournament_fk"),
    )
    var tournament: Tournament,

    @Column(nullable = false, length = 255)
    var name: String,

    @Column(nullable = false, length = 255)
    var slug: String,

    @Column(name = "from_year", nullable = false)
    var fromYear: Int,
)
