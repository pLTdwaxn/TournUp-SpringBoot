package com.abeesmall.tournup.audit

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Table
import org.hibernate.annotations.JdbcTypeCode
import org.hibernate.envers.RevisionEntity
import org.hibernate.envers.RevisionNumber
import org.hibernate.envers.RevisionTimestamp
import org.hibernate.type.SqlTypes
import java.util.UUID

@Entity
@Table(name = "audit_revisions")
@RevisionEntity(AuditRevisionListener::class)
class AuditRevision(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @RevisionNumber
    var id: Int? = null,

    @RevisionTimestamp
    @Column(name = "timestamp_ms", nullable = false)
    var timestampMs: Long = 0,

    @Column(name = "actor_id")
    var actorId: UUID? = null,

    @Column(length = 100)
    var action: String? = null,

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb")
    var context: Map<String, Any?>? = null,
)
