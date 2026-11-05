package com.abeesmall.tournup.tenancy

import com.abeesmall.tournup.credentials.DatabaseCredentialResolver
import com.zaxxer.hikari.HikariDataSource
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.boot.jdbc.autoconfigure.DataSourceProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration(proxyBeanMethods = false)
@ConditionalOnProperty(
    prefix = "tournup.bootstrap.datasource",
    name = ["credential-reference"],
)
@EnableConfigurationProperties(DataSourceProperties::class)
class BootstrapDataSourceConfiguration {

    @Bean(destroyMethod = "close")
    fun dataSource(
        properties: DataSourceProperties,
        @Value("\${tournup.bootstrap.datasource.credential-reference}")
        credentialReference: String,
        credentialResolver: DatabaseCredentialResolver,
    ): HikariDataSource =
        properties.initializeDataSourceBuilder()
            .type(HikariDataSource::class.java)
            .password(credentialResolver.resolvePassword(credentialReference))
            .build()
}
