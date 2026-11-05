package com.abeesmall.tournup.credentials

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.core.env.Environment
import org.springframework.stereotype.Component

interface DatabaseCredentialResolver {
    fun resolvePassword(reference: String): String
}

@Component
@ConditionalOnProperty(
    prefix = "tournup.credentials",
    name = ["provider"],
    havingValue = "env",
    matchIfMissing = true,
)
class EnvironmentDatabaseCredentialResolver(
    private val environment: Environment,
) : DatabaseCredentialResolver {

    private val keyPattern = Regex("[A-Z_][A-Z0-9_]*")

    override fun resolvePassword(reference: String): String {
        require(reference.startsWith("env:")) {
            "Unsupported database credential reference"
        }

        val key = reference.removePrefix("env:")

        require(keyPattern.matches(key)) {
            "Invalid environment credential reference"
        }

        val password = environment.getRequiredProperty(key)

        check(password.isNotEmpty()) {
            "Empty password for environment key $key"
        }

        return password
    }
}
