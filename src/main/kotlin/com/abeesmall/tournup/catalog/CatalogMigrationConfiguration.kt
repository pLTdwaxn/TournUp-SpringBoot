package com.abeesmall.tournup.catalog

import org.flywaydb.core.Flyway
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.boot.jdbc.autoconfigure.DataSourceProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.core.env.Environment

@Configuration(proxyBeanMethods = false)
@ConditionalOnProperty(
    prefix = "tournup.catalog.datasource",
    name = ["url"],
)
class CatalogMigrationConfiguration {

    @Bean(defaultCandidate = false, initMethod = "migrate")
    @ConditionalOnProperty(
        prefix = "tournup.catalog.migration",
        name = ["enabled"],
        havingValue = "true",
    )
    fun catalogFlyway(
        @Qualifier("catalogDataSourceProperties")
        properties: DataSourceProperties,
        environment: Environment,
    ): Flyway =
        Flyway.configure()
            .dataSource(
                requireNotNull(properties.url),
                environment.getRequiredProperty(
                    "tournup.catalog.migration.username",
                ),
                environment.getRequiredProperty(
                    "tournup.catalog.migration.password",
                ),
            )
            .locations("classpath:db/catalog")
            .load()
}
