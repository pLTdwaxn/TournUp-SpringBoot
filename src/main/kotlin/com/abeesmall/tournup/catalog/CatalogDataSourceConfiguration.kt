package com.abeesmall.tournup.catalog

import com.zaxxer.hikari.HikariDataSource
import org.springframework.beans.factory.annotation.Qualifier
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
    fun catalogDataSourceProperties(): DataSourceProperties = DataSourceProperties()

    @Bean(defaultCandidate = false, destroyMethod = "close")
    fun catalogDataSource(
        @Qualifier("catalogDataSourceProperties")
        properties: DataSourceProperties,
    ): HikariDataSource =
        properties.initializeDataSourceBuilder()
            .type(HikariDataSource::class.java)
            .build()
            .apply {
                poolName = "catalog-reader"
                maximumPoolSize = 5
                minimumIdle = 0
                isReadOnly = true
            }
}
