package com.abeesmall.tournup.tenancy

import org.springframework.core.env.Environment
import org.springframework.stereotype.Component

interface TenantCredentialResolver {
    fun resolvePassword(reference: String): String
}

@Component
class EnvironmentTenantCredentialResolver(
    private val environment: Environment,
) : TenantCredentialResolver {

    private val keyPattern = Regex("[A-Z_][A-Z0-9_]*")

    override fun resolvePassword(reference: String): String {
        require(reference.startsWith("env:")) {
            "Unsupported tenant credential reference"
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
