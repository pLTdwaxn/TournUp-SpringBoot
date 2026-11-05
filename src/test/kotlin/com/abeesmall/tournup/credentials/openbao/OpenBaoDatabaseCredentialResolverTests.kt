package com.abeesmall.tournup.credentials.openbao

import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.nio.file.Files
import java.nio.file.Path
import java.time.Duration
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.io.TempDir
import org.mockito.ArgumentCaptor
import org.mockito.Mockito
import org.mockito.Mockito.`when` as whenever
import tools.jackson.databind.json.JsonMapper

class OpenBaoDatabaseCredentialResolverTests {

    @TempDir
    lateinit var directory: Path

    private val httpClient = Mockito.mock(HttpClient::class.java)
    private lateinit var properties: OpenBaoProperties
    private lateinit var resolver: OpenBaoDatabaseCredentialResolver

    @BeforeEach
    fun setUp() {
        properties = OpenBaoProperties(
            address = URI("https://openbao.test:8200"),
            caCertificate = directory.resolve("ca.crt"),
            tokenFile = directory.resolve("token"),
            requestTimeout = Duration.ofSeconds(2),
        )

        Files.writeString(properties.tokenFile, "agent-token-one")

        resolver = OpenBaoDatabaseCredentialResolver(
            properties = properties,
            httpClient = httpClient,
            mapper = JsonMapper.builder().build(),
        )
    }

    @Test
    fun `reads password using the Agent token and KV v2 endpoint`() {
        respond("""{"data":{"data":{"password":"test-db-password"}}}""")

        val password = resolver.resolvePassword(
            "openbao:kv/tournup-api/catalog/database",
        )

        assertEquals("test-db-password", password)

        val request = ArgumentCaptor.forClass(HttpRequest::class.java)

        Mockito.verify(httpClient).send(
            request.capture(),
            Mockito.any<HttpResponse.BodyHandler<String>>(),
        )

        with(request.value) {
            assertEquals("GET", method())
            assertEquals(
                URI(
                    "https://openbao.test:8200" +
                            "/v1/kv/data/tournup-api/catalog/database",
                ),
                uri(),
            )
            assertEquals(
                "agent-token-one",
                headers().firstValue("X-Vault-Token").orElseThrow(),
            )
            assertEquals(
                properties.requestTimeout,
                timeout().orElseThrow(),
            )
        }
    }

    @Test
    fun `reloads the Agent token between lookups`() {
        respond("""{"data":{"data":{"password":"test-db-password"}}}""")

        val reference = "openbao:kv/tournup-api/catalog/database"

        resolver.resolvePassword(reference)
        Files.writeString(properties.tokenFile, "agent-token-two")
        resolver.resolvePassword(reference)

        val requests = ArgumentCaptor.forClass(HttpRequest::class.java)

        Mockito.verify(httpClient, Mockito.times(2)).send(
            requests.capture(),
            Mockito.any<HttpResponse.BodyHandler<String>>(),
        )

        assertEquals(
            listOf("agent-token-one", "agent-token-two"),
            requests.allValues.map {
                it.headers().firstValue("X-Vault-Token").orElseThrow()
            },
        )
    }

    @Test
    fun `denied responses do not expose secret material`() {
        respond(
            body = """
            {"errors":["agent-token-one","SensitivePasswordFromError"]}
        """.trimIndent(),
            status = 403,
        )

        val failure = assertFailsWith<IllegalStateException> {
            resolver.resolvePassword(
                "openbao:kv/tournup-api/catalog/database",
            )
        }

        val trace = failure.stackTraceToString()

        assertTrue("HTTP 403" in trace)
        assertFalse("agent-token-one" in trace)
        assertFalse("SensitivePasswordFromError" in trace)
    }

    @Test
    fun `rejects invalid secret responses without exposing their contents`() {
        val sensitiveValue = "SensitiveMalformedSecretValue"
        val bodies = listOf(
            sensitiveValue,
            """{"data":{"data":{}}}""",
            """{"data":{"data":{"password":null}}}""",
            """{"data":{"data":{"password":123}}}""",
            """{"data":{"data":{"password":""}}}""",
        )

        bodies.forEachIndexed { index, body ->
            respond(body)

            val failure = assertFailsWith<IllegalStateException>(
                "Response case $index must be rejected",
            ) {
                resolver.resolvePassword(
                    "openbao:kv/tournup-api/catalog/database",
                )
            }

            val trace = failure.stackTraceToString()
            assertFalse(sensitiveValue in trace)
            assertFalse("agent-token-one" in trace)
        }
    }

    @Test
    fun `rejects missing or malformed Agent tokens before sending a request`() {
        val reference = "openbao:kv/tournup-api/catalog/database"

        for (token in listOf("", "agent-token-one\r\nInjected: value")) {
            Files.writeString(properties.tokenFile, token)

            assertFailsWith<IllegalStateException> {
                resolver.resolvePassword(reference)
            }
        }

        Files.delete(properties.tokenFile)

        assertFailsWith<IllegalStateException> {
            resolver.resolvePassword(reference)
        }

        Mockito.verifyNoInteractions(httpClient)
    }

    @Test
    fun `rejects unsafe references before sending a request`() {
        val references = listOf(
            "env:DB_PASSWORD",
            "openbao:kv/../other-service/database",
            "openbao:kv/%2e%2e/other-service/database",
            "openbao:kv//database",
            "openbao:kv/database/",
            "openbao:kv/database?version=1",
            "openbao:https://other-host/database",
        )

        for (reference in references) {
            assertFailsWith<IllegalArgumentException> {
                resolver.resolvePassword(reference)
            }
        }

        Mockito.verifyNoInteractions(httpClient)
    }

    @Suppress("UNCHECKED_CAST")
    private fun respond(body: String, status: Int = 200) {
        val response = Mockito.mock(
            HttpResponse::class.java,
        ) as HttpResponse<String>

        whenever(response.statusCode()).thenReturn(status)
        whenever(response.body()).thenReturn(body)

        whenever(
            httpClient.send(
                Mockito.any(HttpRequest::class.java),
                Mockito.any<HttpResponse.BodyHandler<String>>(),
            ),
        ).thenReturn(response)
    }
}
