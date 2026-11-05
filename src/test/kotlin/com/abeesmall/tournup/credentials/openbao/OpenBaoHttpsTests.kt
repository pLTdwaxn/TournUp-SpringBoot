package com.abeesmall.tournup.credentials.openbao

import com.sun.net.httpserver.HttpsConfigurator
import com.sun.net.httpserver.HttpsServer
import java.net.InetSocketAddress
import java.net.URI
import java.nio.file.Files
import java.nio.file.Path
import java.security.KeyStore
import java.util.concurrent.TimeUnit
import javax.net.ssl.KeyManagerFactory
import javax.net.ssl.SSLContext
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.io.TempDir
import tools.jackson.databind.json.JsonMapper

class OpenBaoHttpsTests {

    @TempDir
    lateinit var directory: Path

    private lateinit var server: HttpsServer
    private lateinit var properties: OpenBaoProperties

    @BeforeEach
    fun setUp() {
        val password = "test-only-password".toCharArray()
        val keyStore = generateKeyStore("server", password)

        val certificateFile = directory.resolve("trusted.crt")
        Files.write(
            certificateFile,
            keyStore.getCertificate("server").encoded,
        )

        val keyManagers = KeyManagerFactory.getInstance(
            KeyManagerFactory.getDefaultAlgorithm(),
        ).apply {
            init(keyStore, password)
        }

        val serverContext = SSLContext.getInstance("TLS").apply {
            init(keyManagers.keyManagers, null, null)
        }

        server = HttpsServer.create(
            InetSocketAddress("127.0.0.1", 0),
            0,
        )
        server.httpsConfigurator = HttpsConfigurator(serverContext)

        server.createContext(
            "/v1/kv/data/tournup-api/catalog/database",
        ) { exchange ->
            try {
                val body =
                    """{"data":{"data":{"password":"https-test-password"}}}"""
                        .toByteArray(Charsets.UTF_8)

                exchange.responseHeaders.set(
                    "Content-Type", "application/json",
                )
                exchange.sendResponseHeaders(200, body.size.toLong())
                exchange.responseBody.write(body)
            } finally {
                exchange.close()
            }
        }

        server.start()

        val tokenFile = directory.resolve("token")
        Files.writeString(tokenFile, "test-agent-token")

        properties = OpenBaoProperties(
            address = URI("https://localhost:${server.address.port}"),
            caCertificate = certificateFile,
            tokenFile = tokenFile,
        )
    }

    @AfterEach
    fun tearDown() {
        if (::server.isInitialized) {
            server.stop(0)
        }
    }

    @Test
    fun `reads a secret over HTTPS using the configured certificate trust`() {
        OpenBaoConfiguration().openBaoHttpClient(properties).use { client ->
            val resolver = OpenBaoDatabaseCredentialResolver(
                properties,
                client,
                JsonMapper.builder().build(),
            )

            assertEquals(
                "https-test-password",
                resolver.resolvePassword(
                    "openbao:kv/tournup-api/catalog/database",
                ),
            )
        }
    }

    @Test
    fun `rejects a trusted certificate for the wrong hostname`() {
        val mismatchedProperties = properties.copy(
            address = URI("https://127.0.0.1:${server.address.port}"),
        )

        OpenBaoConfiguration()
            .openBaoHttpClient(mismatchedProperties)
            .use { client ->
                val resolver = OpenBaoDatabaseCredentialResolver(
                    mismatchedProperties,
                    client,
                    JsonMapper.builder().build(),
                )

                val failure = assertFailsWith<IllegalStateException> {
                    resolver.resolvePassword(
                        "openbao:kv/tournup-api/catalog/database",
                    )
                }

                assertEquals(
                    "OpenBao request failed (SSLHandshakeException)",
                    failure.message,
                )
            }
    }

    private fun generateKeyStore(
        name: String,
        password: CharArray,
    ): KeyStore {
        val keyStoreFile = directory.resolve("$name.p12")
        val outputFile = directory.resolve("$name-keytool.log")
        val keytool = Path.of(
            System.getProperty("java.home"), "bin", "keytool",
        )

        val process = ProcessBuilder(
            keytool.toString(),
            "-genkeypair",
            "-alias", "server",
            "-keyalg", "RSA",
            "-keysize", "2048",
            "-storetype", "PKCS12",
            "-keystore", keyStoreFile.toString(),
            "-storepass", String(password),
            "-keypass", String(password),
            "-dname", "CN=localhost",
            "-ext", "SAN=dns:localhost",
            "-validity", "2",
            "-noprompt",
        )
            .redirectErrorStream(true)
            .redirectOutput(outputFile.toFile())
            .start()

        if (!process.waitFor(30, TimeUnit.SECONDS)) {
            process.destroyForcibly()
            error("Test certificate generation timed out")
        }

        assertEquals(
            0,
            process.exitValue(),
            Files.readString(outputFile),
        )

        return KeyStore.getInstance("PKCS12").apply {
            Files.newInputStream(keyStoreFile).use {
                load(it, password)
            }
        }
    }

    @Test
    fun `rejects a server certificate outside the configured trust store`() {
        val unrelatedKeyStore = generateKeyStore(
            "unrelated",
            "test-only-password".toCharArray(),
        )
        val unrelatedCertificate = directory.resolve("unrelated.crt")
        Files.write(
            unrelatedCertificate,
            unrelatedKeyStore.getCertificate("server").encoded,
        )

        val untrustedProperties = properties.copy(
            caCertificate = unrelatedCertificate,
        )

        OpenBaoConfiguration()
            .openBaoHttpClient(untrustedProperties)
            .use { client ->
                val resolver = OpenBaoDatabaseCredentialResolver(
                    untrustedProperties,
                    client,
                    JsonMapper.builder().build(),
                )

                val failure = assertFailsWith<IllegalStateException> {
                    resolver.resolvePassword(
                        "openbao:kv/tournup-api/catalog/database",
                    )
                }

                assertEquals(
                    "OpenBao request failed (SSLHandshakeException)",
                    failure.message,
                )
            }
    }
}
