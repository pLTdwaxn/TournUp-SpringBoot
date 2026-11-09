package com.abeesmall.tournup.repository.audit

import com.abeesmall.tournup.TestcontainersConfiguration
import com.abeesmall.tournup.dto.audit.LastAuditData
import com.abeesmall.tournup.model.tournament.Tournament
import jakarta.persistence.EntityManager
import java.time.Instant
import java.util.UUID
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import org.hibernate.SessionFactory
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase
import org.springframework.context.annotation.Import
import org.springframework.jdbc.core.JdbcTemplate

@DataJpaTest
@AutoConfigureTestDatabase(
    replace = AutoConfigureTestDatabase.Replace.NONE,
)
@Import(TestcontainersConfiguration::class, LatestAuditReader::class)
class LatestAuditReaderTests {
    @Autowired
    lateinit var reader: LatestAuditReader

    @Autowired
    lateinit var jdbc: JdbcTemplate

    @Autowired
    lateinit var entityManager: EntityManager

    @Test
    fun `returns latest metadata per requested record in one query`() {
        val first = UUID.randomUUID()
        val second = UUID.randomUUID()
        val unrequested = UUID.randomUUID()
        val withoutHistory = UUID.randomUUID()
        val unknown = UUID.randomUUID()
        val actor = UUID.randomUUID()

        val earlier = Instant.parse("2026-11-10T00:00:00Z")
        val later = Instant.parse("2026-11-10T01:00:00Z")

        jdbc.update(
            """
            INSERT INTO audit_revisions (id, timestamp_ms, actor_id)
            VALUES (?, ?, NULL)
            """.trimIndent(),
            101,
            earlier.toEpochMilli(),
        )
        jdbc.update(
            """
            INSERT INTO audit_revisions (id, timestamp_ms, actor_id)
            VALUES (?, ?, ?)
            """.trimIndent(),
            102,
            later.toEpochMilli(),
            actor,
        )

        for (id in listOf(first, second, unrequested, withoutHistory)) {
            jdbc.update(
                """
                INSERT INTO tournaments (id, slug, name)
                VALUES (?, ?, ?)
                """.trimIndent(),
                id,
                "fixture-$id",
                "Fixture tournament",
            )
        }

        val history = listOf(
            Triple(first, 101, 0),
            Triple(second, 101, 0),
            Triple(first, 102, 1),
            Triple(unrequested, 102, 0),
        )

        for ((id, revision, type) in history) {
            jdbc.update(
                """
                INSERT INTO tournaments_audit (id, rev, revtype)
                VALUES (?, ?, ?)
                """.trimIndent(),
                id,
                revision,
                type,
            )
        }

        val statistics = entityManager.entityManagerFactory
            .unwrap(SessionFactory::class.java)
            .statistics
        statistics.isStatisticsEnabled = true
        statistics.clear()

        val result = reader.findLatest(
            Tournament::class.java,
            listOf(first, second, withoutHistory, unknown),
        )

        assertEquals(setOf(first, second), result.keys)
        assertEquals(
            LastAuditData(
                revision = 102,
                timestamp = later,
                actorId = actor,
            ),
            result[first],
        )
        assertEquals(
            LastAuditData(
                revision = 101,
                timestamp = earlier,
                actorId = null,
            ),
            result[second],
        )
        assertEquals(1L, statistics.prepareStatementCount)
    }

    @Test
    fun `empty batch returns no metadata without querying`() {
        val statistics = entityManager.entityManagerFactory
            .unwrap(SessionFactory::class.java)
            .statistics
        statistics.isStatisticsEnabled = true
        statistics.clear()

        val result = reader.findLatest(
            Tournament::class.java,
            emptyList(),
        )

        assertTrue(result.isEmpty())
        assertEquals(0L, statistics.prepareStatementCount)
    }
}
