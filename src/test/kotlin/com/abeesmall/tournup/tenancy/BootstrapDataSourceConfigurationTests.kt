package com.abeesmall.tournup.tenancy

import com.abeesmall.tournup.credentials.EnvironmentDatabaseCredentialResolver
import com.zaxxer.hikari.HikariDataSource
import javax.sql.DataSource
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import org.springframework.boot.autoconfigure.AutoConfigurations
import org.springframework.boot.autoconfigure.context.ConfigurationPropertiesAutoConfiguration
import org.springframework.boot.jdbc.autoconfigure.DataSourceAutoConfiguration
import org.springframework.boot.test.context.runner.ApplicationContextRunner

class BootstrapDataSourceConfigurationTests {

    @Test
    fun `creates the startup datasource using the resolved password`() {
        ApplicationContextRunner()
            .withConfiguration(
                AutoConfigurations.of(
                    ConfigurationPropertiesAutoConfiguration::class.java,
                    DataSourceAutoConfiguration::class.java,
                ),
            )
            .withUserConfiguration(
                BootstrapDataSourceConfiguration::class.java,
                EnvironmentDatabaseCredentialResolver::class.java,
            )
            .withPropertyValues(
                "spring.datasource.url=jdbc:postgresql://localhost:5432/bootstrap_test",
                "spring.datasource.username=bootstrap_test_user",
                "tournup.bootstrap.datasource.credential-reference=env:BOOTSTRAP_TEST_PASSWORD",
                "BOOTSTRAP_TEST_PASSWORD=test-only-password",
            )
            .run { context ->
                assertNull(context.startupFailure)
                assertEquals(
                    1,
                    context.getBeansOfType(DataSource::class.java).size,
                )

                val dataSource = context.getBean(
                    "dataSource",
                    HikariDataSource::class.java,
                )

                assertEquals(
                    "jdbc:postgresql://localhost:5432/bootstrap_test",
                    dataSource.jdbcUrl,
                )
                assertEquals("bootstrap_test_user", dataSource.username)
                assertEquals("test-only-password", dataSource.password)
            }
    }
}
