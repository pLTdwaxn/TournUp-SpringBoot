package com.abeesmall.tournup.auth

import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.security.web.authentication.session.SessionAuthenticationStrategy
import org.springframework.security.web.context.SecurityContextRepository
import org.springframework.security.web.csrf.CsrfToken
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/auth")
class AuthController(
    private val authAdapter: AuthAdapter,
    private val securityContextRepository: SecurityContextRepository,
    private val sessionAuthenticationStrategy: SessionAuthenticationStrategy,
) {

    @GetMapping("/config/")
    fun coniguration(): AuthConfiguration = 
        authAdapter.configuration()

    @PostMapping("/login/")
    fun login(
        @RequestBody request: AuthLoginRequest,
        servletRequest: HttpServletRequest,
        servletResponse: HttpServletResponse,
    ): AuthenticatedUser {
        val user = authAdapter.login(request)

        val authorities = user.authorities.map { authority ->
            SimpleGrantedAuthority(authority)
        }

        val authentication = UsernamePasswordAuthenticationToken(
            user,
            null,
            authorities,
        )

        sessionAuthenticationStrategy.onAuthentication(
            authentication,
            servletRequest,
            servletResponse,
        )

        val securityContext =
            SecurityContextHolder.createEmptyContext()

        securityContext.authentication = authentication
        SecurityContextHolder.setContext(securityContext)

        securityContextRepository.saveContext(
            securityContext,
            servletRequest,
            servletResponse,
        )


        return user
    }

    @GetMapping("/me/")
    fun currentUser(
        @AuthenticationPrincipal user: AuthenticatedUser,
    ): AuthenticatedUser =
        user

    @GetMapping("/csrf/")
    fun csrfToken(
        csrfToken: CsrfToken,
    ): CsrfToken =
        csrfToken
}
