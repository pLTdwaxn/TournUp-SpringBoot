package com.abeesmall.tournup.security

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.http.HttpStatus
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.web.SecurityFilterChain
import org.springframework.security.web.authentication.HttpStatusEntryPoint
import org.springframework.security.web.authentication.logout.HttpStatusReturningLogoutSuccessHandler
import org.springframework.security.web.authentication.session.ChangeSessionIdAuthenticationStrategy
import org.springframework.security.web.authentication.session.SessionAuthenticationStrategy
import org.springframework.security.web.context.HttpSessionSecurityContextRepository
import org.springframework.security.web.context.SecurityContextRepository

@Configuration(proxyBeanMethods = false)
class SecurityConfiguration {

    @Bean
    fun securityFilterChain(
        http: HttpSecurity,
        securityContextRepository: SecurityContextRepository,
    ): SecurityFilterChain {
        http.securityContext { securityContext ->
            securityContext.securityContextRepository(
                securityContextRepository,
            )
        }

        http.authorizeHttpRequests { requests ->
            requests
                .requestMatchers(
                    "/api/health/",
                    "/api/auth/config/",
                    "/api/auth/csrf/",
                    "/api/auth/login/",
                ).permitAll()
                .anyRequest().authenticated()
        }

        http.exceptionHandling { exceptions ->
            exceptions.authenticationEntryPoint(
                HttpStatusEntryPoint(HttpStatus.UNAUTHORIZED),
            )
        }

        http.logout { logout ->
            logout
                .logoutUrl("/api/auth/logout/")
                .logoutSuccessHandler(
                    HttpStatusReturningLogoutSuccessHandler(
                        HttpStatus.NO_CONTENT,
                    ),
                )
                .invalidateHttpSession(true)
                .clearAuthentication(true)
                .deleteCookies("JSESSIONID")
        }

        return http.build()
    }

    @Bean
    fun securityContextRepository(): SecurityContextRepository =
        HttpSessionSecurityContextRepository()

    @Bean
    fun sessionAuthenticationStrategy(): SessionAuthenticationStrategy =
        ChangeSessionIdAuthenticationStrategy()
}
