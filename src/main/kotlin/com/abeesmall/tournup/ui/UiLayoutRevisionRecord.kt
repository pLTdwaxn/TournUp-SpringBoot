package com.abeesmall.tournup.ui

import jakarta.persistence.Column
import jakarta.persistence.Embeddable
import jakarta.persistence.EmbeddedId
import jakarta.persistence.Entity
import jakarta.persistence.Table
import org.hibernate.annotations.JdbcTypeCode
import org.hibernate.type.SqlTypes
import java.io.Serializable
import java.util.UUID

@Embeddable
data class UiLayoutRevisionId(
    @Column(name = "layout_id")
    val layoutId: UUID,

    @Column(name = "revision")
    val revision: Int,
) : Serializable

@Entity
@Table(name = "ui_layout_revisions")
class UiLayoutRevisionRecord(
    @EmbeddedId
    val id: UiLayoutRevisionId,

    @Column(name = "contract_version", nullable = false)
    val contractVersion: Int,

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "definition", nullable = false, columnDefinition = "jsonb")
    val definition: LayoutDefinition,
)
