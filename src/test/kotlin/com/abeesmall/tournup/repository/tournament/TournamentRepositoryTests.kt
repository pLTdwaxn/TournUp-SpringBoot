package com.abeesmall.tournup.repository.tournament

import com.abeesmall.tournup.TestcontainersConfiguration
import com.abeesmall.tournup.model.tournament.Tournament
import com.abeesmall.tournup.service.LookupQuery
import java.util.UUID
import kotlin.test.assertNull
import jakarta.persistence.EntityManager
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNotSame
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase
import org.springframework.context.annotation.Import

@DataJpaTest
@AutoConfigureTestDatabase(
    replace = AutoConfigureTestDatabase.Replace.NONE,
)
@Import(TestcontainersConfiguration::class)
class TournamentRepositoryTests {
    @Autowired
    lateinit var dataSource: javax.sql.DataSource

    @Test
    fun `database rejects duplicate NanoIDs regardless of slug`() {
        repository.saveAndFlush(Tournament(name = "First", slug = "first", nanoId = "BCDF"))
        kotlin.test.assertFailsWith<org.springframework.dao.DataIntegrityViolationException> {
            repository.saveAndFlush(Tournament(name = "Second", slug = "second", nanoId = "BCDF"))
        }
    }

    @Test
    fun `identity reset discards resources and history while preserving shared tables`() {
        val schema = "identity_test_" + UUID.randomUUID().toString().replace("-", "")
        val flyway = org.flywaydb.core.Flyway.configure().dataSource(dataSource)
            .schemas(schema).defaultSchema(schema)
        try {
            flyway.target("4").load().migrate()
            dataSource.connection.use { connection ->
                connection.createStatement().use { sql ->
                    sql.execute("INSERT INTO $schema.audit_revisions(id, timestamp_ms) VALUES (7, 1)")
                    sql.execute("INSERT INTO $schema.tournaments(id, slug, name) VALUES ('00000000-0000-4000-8000-000000000001', 'old', 'Old')")
                    sql.execute("INSERT INTO $schema.seasons(id, tournament_id, name, slug, from_year) VALUES ('00000000-0000-4000-8000-000000000002', '00000000-0000-4000-8000-000000000001', 'Old', 'old', 2026)")
                    sql.execute("INSERT INTO $schema.tournaments_audit(id, rev) VALUES ('00000000-0000-4000-8000-000000000001', 7)")
                    sql.execute("INSERT INTO $schema.seasons_audit(id, rev) VALUES ('00000000-0000-4000-8000-000000000002', 7)")
                    sql.execute("INSERT INTO $schema.ui_layouts(id, application, resource, operation) VALUES ('00000000-0000-4000-8000-000000000003', 'test', 'tournament', 'list')")
                }
            }
            flyway.target("5").load().migrate()
            // Previously applied V5 must validate unchanged and upgrade through V6/V7.
            dataSource.connection.use { connection ->
                connection.createStatement().use { sql ->
                    sql.execute("INSERT INTO $schema.tournaments(id, short_id, slug, name) VALUES ('00000000-0000-4000-8000-000000000001', 'AbCd01234567', 'old', 'Old')")
                    sql.execute("INSERT INTO $schema.seasons(id, short_id, tournament_id, name, slug, from_year) VALUES ('00000000-0000-4000-8000-000000000002', 'ZyXw98765432', '00000000-0000-4000-8000-000000000001', 'Old', 'old', 2026)")
                    sql.execute("INSERT INTO $schema.tournaments_audit(id, short_id, rev) VALUES ('00000000-0000-4000-8000-000000000001', 'AbCd01234567', 7)")
                    sql.execute("INSERT INTO $schema.seasons_audit(id, short_id, rev) VALUES ('00000000-0000-4000-8000-000000000002', 'ZyXw98765432', 7)")
                }
            }
            // Reproduce the dev database: retired Java V5 followed by the SQL V5 reset.
            dataSource.connection.use { connection ->
                connection.createStatement().use { sql ->
                    sql.execute("UPDATE $schema.flyway_schema_history SET installed_rank = 6 WHERE version = '5' AND type = 'SQL'")
                    sql.execute("INSERT INTO $schema.flyway_schema_history(installed_rank, version, description, type, script, installed_by, execution_time, success) VALUES (5, '5', 'resource identifiers', 'JDBC', 'db.migration.V5__resource_identifiers', CURRENT_USER, 0, true)")
                }
            }
            kotlin.test.assertFalse(flyway.target("7").load().validateWithResult().validationSuccessful)
            dataSource.connection.use { connection ->
                connection.createStatement().use { sql ->
                    assertEquals(1, sql.executeUpdate("DELETE FROM $schema.flyway_schema_history WHERE version = '5' AND type = 'JDBC' AND script = 'db.migration.V5__resource_identifiers' AND checksum IS NULL AND success"))
                }
            }
            flyway.target("7").load().migrate()
            kotlin.test.assertTrue(flyway.target("7").load().validateWithResult().validationSuccessful)
            dataSource.connection.use { connection ->
                connection.createStatement().use { sql ->
                    for (table in listOf("tournaments", "seasons", "tournaments_audit", "seasons_audit")) {
                        sql.executeQuery("SELECT count(*) FROM $schema.$table").use { rows ->
                            rows.next(); assertEquals(0L, rows.getLong(1), table)
                        }
                    }
                    for (table in listOf("tournaments", "seasons")) {
                        sql.executeQuery("SELECT column_name, character_maximum_length FROM information_schema.columns WHERE table_schema = '$schema' AND table_name = '$table' AND column_name IN ('short_id', 'nano_id')").use { rows ->
                            kotlin.test.assertTrue(rows.next())
                            assertEquals("nano_id", rows.getString(1))
                            assertEquals(4, rows.getInt(2))
                            kotlin.test.assertFalse(rows.next())
                        }
                    }
                    for (table in listOf("audit_revisions", "ui_layouts")) {
                        sql.executeQuery("SELECT count(*) FROM $schema.$table").use { rows ->
                            rows.next(); assertEquals(1L, rows.getLong(1), table)
                        }
                    }
                }
            }
        } finally {
            dataSource.connection.use { it.createStatement().use { sql -> sql.execute("DROP SCHEMA IF EXISTS $schema CASCADE") } }
        }
    }

    @Test
    fun `public identity persists independently of UUIDv7 and survives slug changes`() {
        val item = repository.saveAndFlush(Tournament(slug = "original", name = "Original"))
        val id = item.id
        val nanoId = item.nanoId
        assertEquals(7, id.version())
        item.slug = "renamed"
        entityManager.flush()
        entityManager.clear()
        val saved = assertNotNull(repository.findByNanoId(nanoId))
        assertEquals(id, saved.id)
        assertEquals("renamed-$nanoId", saved.locator)
    }

    @Autowired
    lateinit var repository: TournamentRepository

    @Autowired
    lateinit var entityManager: EntityManager

    @Test
    fun `lookup projects display fields and continues through duplicate names`() {
        val prefix = "lookup-${UUID.randomUUID()}"
        val records = listOf("A", "A", "B").map { suffix ->
            repository.saveAndFlush(Tournament(slug = "lookup-${UUID.randomUUID()}", name = "$prefix $suffix"))
        }
        entityManager.clear()
        val first = LookupQuery.parse("tournament", prefix, 2, null)
        val batch = first.response(repository.lookup(first.pattern, first.afterName, first.afterId, first.pageable))
        assertEquals(records.take(2).map { it.locator }.toSet(), batch.items.map { it.id }.toSet())
        val next = LookupQuery.parse("tournament", prefix, 2, assertNotNull(batch.nextCursor))
        val last = next.response(repository.lookup(next.pattern, next.afterName, next.afterId, next.pageable))
        assertEquals(listOf(records.last().locator), last.items.map { it.id })
        assertNull(last.nextCursor)
    }

    @Test
    fun `saves and finds a tournament by slug`() {
        val tournament = Tournament(
            slug = "spring-championship",
            name = "Spring Championship",
        )

        repository.saveAndFlush(tournament)
        entityManager.clear()

        val found = assertNotNull(
            repository.findBySlug("spring-championship"),
        )

        assertNotSame(tournament, found)

        assertEquals(tournament.id, found.id)
        assertEquals("Spring Championship", found.name)
    }

    @Test
    fun `updates a managed tournament`() {
        val tournament = repository.saveAndFlush(
            Tournament(
                slug = "winter-championship",
                name = "Winter Championship",
            ),
        )

        tournament.name = "Winter Open"
        entityManager.flush()
        entityManager.clear()

        val found = assertNotNull(repository.findById(tournament.id).orElse(null))

        assertEquals("Winter Open", found.name)
    }
}
