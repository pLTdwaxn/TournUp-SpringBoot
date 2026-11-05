package com.abeesmall.tournup.catalog

import com.abeesmall.tournup.credentials.DatabaseCredentialResolver

import com.zaxxer.hikari.HikariDataSource
import org.flywaydb.core.Flyway
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.beans.factory.annotation.Value
import org.springframework.beans.factory.ObjectProvider
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.boot.jdbc.autoconfigure.DataSourceProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration(proxyBeanMethods = false)
@ConditionalOnProperty(
    prefix = "tournup.catalog.datasource",
    name = ["url"],
)
class CatalogDataSourceConfiguration {

    @Bean(defaultCandidate = false)
    @ConfigurationProperties("tournup.catalog.datasource")
    fun catalogDataSourceProperties(): DataSourceProperties =
        DataSourceProperties()

    @Bean(defaultCandidate = false, destroyMethod = "close")
    fun catalogDataSource(
        @Qualifier("catalogDataSourceProperties")
        properties: DataSourceProperties,
        @Value("\${tournup.catalog.datasource.credential-reference}")
        credentialReference: String,
        credentialResolver: DatabaseCredentialResolver,
        @Qualifier("catalogFlyway")
        catalogMigration: ObjectProvider<Flyway>,
    ): HikariDataSource {
        // Run enabled migrations before making the reader available.
        catalogMigration.getIfAvailable()

        return properties.initializeDataSourceBuilder()
            .type(HikariDataSource::class.java)
            .password(credentialResolver.resolvePassword(credentialReference))
            .build()
            .apply {
                poolName = "catalog-reader"
                maximumPoolSize = 5
                minimumIdle = 0
                isReadOnly = true
            }
    }
}
