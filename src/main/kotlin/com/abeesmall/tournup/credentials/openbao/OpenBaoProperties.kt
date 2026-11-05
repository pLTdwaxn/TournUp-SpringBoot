package com.abeesmall.tournup.credentials.openbao

import java.net.URI
import java.nio.file.Path
import java.time.Duration
import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties("tournup.credentials.openbao")
data class OpenBaoProperties(
    val address: URI,
    val caCertificate: Path,
    val tokenFile: Path,
    val connectTimeout: Duration = Duration.ofSeconds(5),
    val requestTimeout: Duration = Duration.ofSeconds(5),
) {
    init {
        require(address.scheme == "https" && !address.host.isNullOrBlank()) {
            "OpenBao address must be an HTTPS URL with a hostname"
        }

        require(
            address.rawUserInfo == null &&
                    address.rawQuery == null &&
                    address.rawFragment == null &&
                    address.rawPath.orEmpty() in setOf("", "/"),
        ) {
            "OpenBao address must contain only the scheme, host, and optional port"
        }

        require(
            connectTimeout > Duration.ZERO &&
                    requestTimeout > Duration.ZERO,
        ) {
            "OpenBao timeouts must be positive"
        }
    }
}
