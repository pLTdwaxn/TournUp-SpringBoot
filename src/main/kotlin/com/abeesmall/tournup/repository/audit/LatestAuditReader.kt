package com.abeesmall.tournup.repository.audit

import com.abeesmall.tournup.dto.audit.LastAuditData
import jakarta.persistence.EntityManager
import java.time.Instant
import java.util.UUID
import org.hibernate.envers.AuditReaderFactory
import org.hibernate.envers.query.AuditEntity
import org.springframework.stereotype.Repository
import org.springframework.transaction.annotation.Transactional

@Repository
class LatestAuditReader(
    private val entityManager: EntityManager,
) {
    @Transactional(readOnly = true)
    fun findLatest(
        entityClass: Class<*>,
        recordIds: Collection<UUID>,
    ): Map<UUID, LastAuditData> {
        if (recordIds.isEmpty()) {
            return emptyMap()
        }

        val rows = AuditReaderFactory.get(entityManager)
            .createQuery()
            .forRevisionsOfEntity(entityClass, false, true)
            .add(AuditEntity.id().`in`(recordIds))
            .add(
                AuditEntity.revisionNumber()
                    .maximize()
                    .computeAggregationInInstanceContext(),
            )
            .addProjection(AuditEntity.id())
            .addProjection(AuditEntity.revisionNumber())
            .addProjection(AuditEntity.revisionProperty("timestampMs"))
            .addProjection(AuditEntity.revisionProperty("actorId"))
            .resultList

        return rows.associate { result ->
            val row = result as Array<*>

            (row[0] as UUID) to LastAuditData(
                revision = (row[1] as Number).toInt(),
                timestamp = Instant.ofEpochMilli(
                    (row[2] as Number).toLong(),
                ),
                actorId = row[3] as UUID?,
            )
        }
    }
}
