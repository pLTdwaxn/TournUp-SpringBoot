package com.abeesmall.tournup.ui

import org.springframework.data.jpa.repository.JpaRepository
import java.util.UUID

interface UiLayoutRepository : JpaRepository<UiLayoutRecord, UUID> {
    fun findByApplicationAndResourceAndOperation(
        application: String,
        resource: String,
        operation: String,
    ): UiLayoutRecord?
}
