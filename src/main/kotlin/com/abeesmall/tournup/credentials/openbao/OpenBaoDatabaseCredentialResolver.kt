package com.abeesmall.tournup.credentials.openbao

import com.abeesmall.tournup.credentials.DatabaseCredentialResolver
import java.io.IOException
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.nio.charset.StandardCharsets
import java.nio.file.Files
import tools.jackson.core.JacksonException
import tools.jackson.databind.json.JsonMapper


class OpenBaoDatabaseCredentialResolver(
    private val properties: OpenBaoProperties,
    private val httpClient: HttpClient,
    private val mapper: JsonMapper,
) : DatabaseCredentialResolver {

    override fun resolvePassword(reference: String): String {
        val secret = OpenBaoSecretReference.parse(reference)

        val request = HttpRequest.newBuilder(
            properties.address.resolve(secret.apiPath),
        )
            .timeout(properties.requestTimeout)
            .header("Accept", "application/json")
            .header("X-Vault-Token", readToken())
            .GET()
            .build()

        val response = try {
            httpClient.send(
                request,
                HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8),
            )
        } catch (exception: IOException) {
            throw IllegalStateException(
                "OpenBao request failed (${exception.javaClass.simpleName})",
            )
        } catch (_: InterruptedException) {
            Thread.currentThread().interrupt()
            throw IllegalStateException("OpenBao request was interrupted")
        }

        check(response.statusCode() == 200) {
            "OpenBao secret lookup failed (HTTP ${response.statusCode()})"
        }

        val passwordNode = try {
            mapper.readTree(response.body()).at("/data/data/password")
        } catch (_: JacksonException) {
            throw IllegalStateException(
                "OpenBao returned an invalid secret response",
            )
        }

        check(passwordNode.isString) {
            "OpenBao secret must contain a string password field"
        }

        val password = passwordNode.stringValue()

        check(password.isNotEmpty()) {
            "OpenBao secret contains an empty password"
        }

        return password
    }

    private fun readToken(): String {
        val token = try {
            Files.readString(properties.tokenFile).trim()
        } catch (_: IOException) {
            throw IllegalStateException("Cannot read OpenBao Agent token file")
        }

        check(token.isNotEmpty() && token.all { it in '!'..'~' }) {
            "OpenBao Agent token file contains an invalid token"
        }

        return token
    }
}
