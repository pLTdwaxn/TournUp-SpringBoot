package com.abeesmall.tournup.catalog

import com.abeesmall.tournup.credentials.EnvironmentDatabaseCredentialResolver

import java.util.UUID
import java.util.function.Supplier
import javax.sql.DataSource
import org.flywaydb.core.Flyway
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.mockito.Mockito.mock
import org.springframework.boot.autoconfigure.AutoConfigurations
import org.springframework.boot.autoconfigure.context.ConfigurationPropertiesAutoConfiguration
import org.springframework.boot.test.context.runner.ApplicationContextRunner
import org.springframework.jdbc.core.simple.JdbcClient
import org.springframework.jdbc.datasource.DriverManagerDataSource
import org.testcontainers.postgresql.PostgreSQLContainer
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class JdbcTenantCatalogTests {

    private val postgres =
        PostgreSQLContainer(
            "postgres@sha256:77f585114c32fbca283dc835b0596f4e52b51b4c6662d7810b2f4084f60a1873",
        )
            .withDatabaseName("catalog_test")
            .withUsername("catalog_test_owner")
            .withPassword("catalog_test_owner_password")
            .withInitScript("catalog/init.sql")

    private val tenant = CatalogTenant(
        id = UUID.randomUUID(),
        slug = "catalog-test",
        name = "Catalog test tenant",
    )

    private val hostname = "catalog-test.preview.example"

    private val binding = TenantDatabaseBinding(
        tenantId = tenant.id,
        serviceName = "tournup-api",
        databaseHost = "tenant-db.internal",
        databasePort = 5544,
        databaseName = "catalog_test_tenant",
        databaseUsername = "catalog_test_tenant_app",
        credentialReference = "test:catalog-test",
        configurationVersion = 7L,
    )

    private lateinit var admin: JdbcClient

    @BeforeAll
    fun startPostgres() {
        postgres.start()
        admin = JdbcClient.create(
            DriverManagerDataSource(
                postgres.jdbcUrl,
                postgres.username,
                postgres.password,
            ),
        )
    }

    @AfterAll
    fun stopPostgres() {
        postgres.stop()
    }

    @Test
    fun `reads active tenant and binding using the catalog reader`() {
        withCatalog { catalog, readerDataSource ->
            assertEquals(tenant, catalog.findActiveTenantById(tenant.id))
            assertEquals(tenant, catalog.findActiveTenantBySlug(tenant.slug))
            assertEquals(
                tenant,
                catalog.findActiveTenantByHostname(
                    hostname.uppercase() + ".",
                ),
            )
            assertEquals(
                binding,
                catalog.findActiveDatabaseBinding(
                    tenant.id,
                    binding.serviceName,
                ),
            )

            val reader = JdbcClient.create(readerDataSource)
            assertEquals(
                "catalog_test_reader",
                reader.sql("SELECT current_user")
                    .query(String::class.java)
                    .single(),
            )
            assertEquals(
                false,
                reader.sql(
                    """
                    SELECT has_table_privilege(
                        current_user,
                        'public.tenants',
                        'INSERT, UPDATE, DELETE, TRUNCATE'
                    )
                    """.trimIndent(),
                )
                    .query(Boolean::class.javaObjectType)
                    .single(),
            )
        }
    }

    @Test
    fun `returns null for unknown tenants hostnames and services`() {
        withCatalog { catalog, _ ->
            val missingId = UUID.randomUUID()

            assertNull(catalog.findActiveTenantById(missingId))
            assertNull(catalog.findActiveTenantBySlug("missing"))
            assertNull(catalog.findActiveTenantBySlug("' OR 1=1 --"))
            assertNull(
                catalog.findActiveTenantByHostname("missing.preview.example"),
            )
            assertNull(
                catalog.findActiveDatabaseBinding(missingId, binding.serviceName),
            )
            assertNull(
                catalog.findActiveDatabaseBinding(tenant.id, "other-service"),
            )
        }
    }

    @Test
    fun `excludes inactive bindings and inactive tenants`() {
        withCatalog { catalog, _ ->
            admin.sql(
                "UPDATE public.tenant_database_bindings SET status = 'SUSPENDED'",
            ).update()

            assertEquals(tenant, catalog.findActiveTenantById(tenant.id))
            assertNull(
                catalog.findActiveDatabaseBinding(tenant.id, binding.serviceName),
            )

            admin.sql(
                "UPDATE public.tenant_database_bindings SET status = 'ACTIVE'",
            ).update()
            admin.sql(
                "UPDATE public.tenants SET status = 'SUSPENDED'",
            ).update()

            assertNull(catalog.findActiveTenantById(tenant.id))
            assertNull(catalog.findActiveTenantBySlug(tenant.slug))
            assertNull(catalog.findActiveTenantByHostname(hostname))
            assertNull(
                catalog.findActiveDatabaseBinding(tenant.id, binding.serviceName),
            )
        }
    }

    @Test
    fun `lists only active bindings for the requested service`() {
        withCatalog { catalog, _ ->
            assertEquals(
                listOf(binding),
                catalog.findActiveDatabaseBindings(binding.serviceName),
            )
            assertEquals(
                emptyList(),
                catalog.findActiveDatabaseBindings("other-service"),
            )

            admin.sql(
                "UPDATE public.tenant_database_bindings SET status = 'SUSPENDED'",
            ).update()

            assertEquals(
                emptyList(),
                catalog.findActiveDatabaseBindings(binding.serviceName),
            )

            admin.sql(
                "UPDATE public.tenant_database_bindings SET status = 'ACTIVE'",
            ).update()
            admin.sql(
                "UPDATE public.tenants SET status = 'SUSPENDED'",
            ).update()

            assertEquals(
                emptyList(),
                catalog.findActiveDatabaseBindings(binding.serviceName),
            )
        }
    }

    @Test
    fun `reads an existing catalog without migration credentials`() {
        // Provision the schema separately from the application context.
        Flyway.configure()
            .dataSource(postgres.jdbcUrl, postgres.username, postgres.password)
            .locations("classpath:db/catalog")
            .load()
            .migrate()

        ApplicationContextRunner()
            .withConfiguration(
                AutoConfigurations.of(
                    ConfigurationPropertiesAutoConfiguration::class.java,
                ),
            )
            .withUserConfiguration(
                EnvironmentDatabaseCredentialResolver::class.java,
                CatalogDataSourceConfiguration::class.java,
                CatalogMigrationConfiguration::class.java,
                JdbcTenantCatalog::class.java,
            )
            .withPropertyValues(
                "tournup.catalog.datasource.url=${postgres.jdbcUrl}",
                "tournup.catalog.datasource.username=catalog_test_reader",
                "tournup.catalog.datasource.credential-reference=env:CATALOG_DB_PASSWORD",
                "CATALOG_DB_PASSWORD=catalog_test_reader_password",
                "tournup.catalog.migration.enabled=false",
            )
            .run { context ->
                assertNull(context.startupFailure)
                assertFalse(context.containsBean("catalogFlyway"))

                seedCatalog()

                val catalog = context.getBean(TenantCatalog::class.java)
                assertEquals(tenant, catalog.findActiveTenantById(tenant.id))
            }
    }

    private fun withCatalog(
        assertions: (TenantCatalog, DataSource) -> Unit,
    ) {
        ApplicationContextRunner()
            .withConfiguration(
                AutoConfigurations.of(
                    ConfigurationPropertiesAutoConfiguration::class.java,
                ),
            )
            .withUserConfiguration(
                EnvironmentDatabaseCredentialResolver::class.java,
                CatalogDataSourceConfiguration::class.java,
                CatalogMigrationConfiguration::class.java,
                JdbcTenantCatalog::class.java,
            )
            .withBean(
                "dataSource",
                DataSource::class.java,
                Supplier { mock(DataSource::class.java) },
            )
            .withPropertyValues(
                "tournup.catalog.datasource.url=${postgres.jdbcUrl}",
                "tournup.catalog.datasource.username=catalog_test_reader",
                "tournup.catalog.datasource.credential-reference=env:CATALOG_DB_PASSWORD",
                "CATALOG_DB_PASSWORD=catalog_test_reader_password",
                "tournup.catalog.migration.enabled=true",
                "tournup.catalog.migration.username=${postgres.username}",
                "tournup.catalog.migration.password=${postgres.password}",
            )
            .run { context ->
                assertNull(context.startupFailure)
                seedCatalog()

                assertions(
                    context.getBean(TenantCatalog::class.java),
                    context.getBean("catalogDataSource", DataSource::class.java),
                )
            }
    }

    private fun seedCatalog() {
        admin.sql(
            """
            TRUNCATE public.tenant_domains,
                     public.tenant_database_bindings,
                     public.tenants
            """.trimIndent(),
        ).update()

        admin.sql(
            """
            INSERT INTO public.tenants (id, slug, name, status)
            VALUES (:id, :slug, :name, 'ACTIVE')
            """.trimIndent(),
        )
            .paramSource(tenant)
            .update()

        admin.sql(
            """
            INSERT INTO public.tenant_domains (hostname, tenant_id)
            VALUES (:hostname, :tenantId)
            """.trimIndent(),
        )
            .param("hostname", hostname)
            .param("tenantId", tenant.id)
            .update()

        admin.sql(
            """
            INSERT INTO public.tenant_database_bindings (
                tenant_id, service_name, database_host, database_port,
                database_name, database_username, credential_reference,
                status, configuration_version
            )
            VALUES (
                :tenantId, :serviceName, :databaseHost, :databasePort,
                :databaseName, :databaseUsername, :credentialReference,
                'ACTIVE', :configurationVersion
            )
            """.trimIndent(),
        )
            .paramSource(binding)
            .update()
    }
}
