package com.abeesmall.tournup.tenancy

import com.abeesmall.tournup.catalog.TenantCatalog

import java.sql.Connection
import org.flywaydb.core.Flyway
import org.springframework.boot.flyway.autoconfigure.FlywayMigrationStrategy
import org.springframework.boot.flyway.autoconfigure.FlywayProperties
import org.springframework.context.annotation.Profile
import org.springframework.stereotype.Component

@Component
@Profile("dev")
class TenantMigrationStrategy(
    private val catalog: TenantCatalog,
    private val migrationProperties: FlywayProperties,
) : FlywayMigrationStrategy {

    override fun migrate(flyway: Flyway) {
        flyway.migrate()

        val base = flyway.configuration

        catalog.findActiveDatabaseBindings("tournup-api")
            .groupBy { it.toPostgresJdbcUrl() }
            .forEach { (url, bindings) ->
                val tenantFlyway = if (url == migrationProperties.url) flyway else Flyway.configure()
                    .configuration(base)
                    .dataSource(url, migrationProperties.user, migrationProperties.password)
                    .load().also { it.migrate() }
                tenantFlyway.configuration.dataSource.connection.use { connection ->
                    bindings.map { it.databaseUsername }.distinct()
                        .filter { it != migrationProperties.user }
                        .forEach { role ->
                            grantRuntimeAccess(connection, role, base.defaultSchema ?: "public", base.table)
                        }
                }
            }
    }

    private fun grantRuntimeAccess(connection: Connection, username: String, schema: String, historyTable: String) {
        fun identifier(value: String) = "\"${value.replace("\"", "\"\"")}\""
        val role = identifier(username)
        val namespace = identifier(schema)
        val tables = connection.prepareStatement(
            "SELECT tablename FROM pg_tables WHERE schemaname = ? AND tablename <> ?",
        ).use { query ->
            query.setString(1, schema)
            query.setString(2, historyTable)
            query.executeQuery().use { rows ->
                buildList { while (rows.next()) add("$namespace.${identifier(rows.getString(1))}") }
            }
        }
        connection.createStatement().use { sql ->
            sql.execute("GRANT USAGE ON SCHEMA $namespace TO $role")
            if (tables.isNotEmpty()) {
                sql.execute("GRANT SELECT, INSERT, UPDATE, DELETE ON TABLE ${tables.joinToString()} TO $role")
            }
            sql.execute("GRANT USAGE, SELECT ON ALL SEQUENCES IN SCHEMA $namespace TO $role")
            // Defaults belong to the migration role creating future tables, not the runtime role.
            sql.execute("ALTER DEFAULT PRIVILEGES IN SCHEMA $namespace GRANT SELECT, INSERT, UPDATE, DELETE ON TABLES TO $role")
            sql.execute("ALTER DEFAULT PRIVILEGES IN SCHEMA $namespace GRANT USAGE, SELECT ON SEQUENCES TO $role")
        }
    }
}
