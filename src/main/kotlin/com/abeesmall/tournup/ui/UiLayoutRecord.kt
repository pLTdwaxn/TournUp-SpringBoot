package com.abeesmall.tournup.ui

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.Table
import java.util.UUID

@Entity
@Table(name = "ui_layouts")
class UiLayoutRecord(
    @Id
    val id: UUID = UUID.randomUUID(),

    @Column(nullable = false, length = 100)
    val application: String,

    @Column(nullable = false, length = 100)
    val resource: String,

    @Column(nullable = false, length = 30)
    val operation: String,

    @Column(name = "published_revision")
    var publishedRevision: Int? = null,
)
