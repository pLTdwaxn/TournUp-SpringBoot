package com.abeesmall.tournup.tenancy

import com.abeesmall.tournup.catalog.TenantCatalog

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
            .map { it.toPostgresJdbcUrl() }
            .distinct()
            .filter { it != migrationProperties.url }
            .forEach { url ->
                Flyway.configure()
                    .configuration(base)
                    .dataSource(
                        url,
                        migrationProperties.user,
                        migrationProperties.password,
                    )
                    .load()
                    .migrate()
            }
    }
}
