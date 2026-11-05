package com.abeesmall.tournup.credentials.openbao

import com.abeesmall.tournup.credentials.DatabaseCredentialResolver
import java.net.http.HttpClient
import java.nio.file.Files
import java.security.KeyStore
import java.security.cert.CertificateFactory
import javax.net.ssl.SSLContext
import javax.net.ssl.TrustManagerFactory
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import tools.jackson.databind.json.JsonMapper

@Configuration(proxyBeanMethods = false)
@ConditionalOnProperty(
    prefix = "tournup.credentials",
    name = ["provider"],
    havingValue = "openbao",
)
@EnableConfigurationProperties(OpenBaoProperties::class)
class OpenBaoConfiguration {

    @Bean(defaultCandidate = false, destroyMethod = "close")
    fun openBaoHttpClient(properties: OpenBaoProperties): HttpClient {
        val certificates = Files.newInputStream(properties.caCertificate).use {
            CertificateFactory.getInstance("X.509").generateCertificates(it)
        }

        check(certificates.isNotEmpty()) {
            "OpenBao CA certificate file contains no certificates"
        }

        val trustStore = KeyStore.getInstance(KeyStore.getDefaultType()).apply {
            load(null, null)
        }

        certificates.forEachIndexed { index, certificate ->
            trustStore.setCertificateEntry("openbao-ca-$index", certificate)
        }

        val trustManagers = TrustManagerFactory.getInstance(
            TrustManagerFactory.getDefaultAlgorithm(),
        ).apply {
            init(trustStore)
        }

        val sslContext = SSLContext.getInstance("TLS").apply {
            init(null, trustManagers.trustManagers, null)
        }

        val sslParameters = sslContext.defaultSSLParameters.apply {
            endpointIdentificationAlgorithm = "HTTPS"
        }

        return HttpClient.newBuilder()
            .sslContext(sslContext)
            .sslParameters(sslParameters)
            .connectTimeout(properties.connectTimeout)
            .followRedirects(HttpClient.Redirect.NEVER)
            .build()
    }

    @Bean
    fun openBaoCredentialResolver(
        properties: OpenBaoProperties,
        @Qualifier("openBaoHttpClient") httpClient: HttpClient,
        mapper: JsonMapper,
    ): DatabaseCredentialResolver =
        OpenBaoDatabaseCredentialResolver(
            properties = properties,
            httpClient = httpClient,
            mapper = mapper,
        )
}
