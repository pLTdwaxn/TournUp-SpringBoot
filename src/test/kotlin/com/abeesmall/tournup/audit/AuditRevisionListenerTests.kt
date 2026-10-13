package com.abeesmall.tournup.audit

import com.abeesmall.tournup.auth.AuthenticatedUser
import org.junit.jupiter.api.Test
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.context.SecurityContextHolder
import java.util.UUID
import kotlin.test.assertEquals
import kotlin.test.assertNull

class AuditRevisionListenerTests {
    private val listener = AuditRevisionListener()

    @Test
    fun `records the authenticated user as actor`() {
        val user = AuthenticatedUser(
            userId = UUID.randomUUID(),
            displayName = "Test User",
            tenantId = UUID.randomUUID(),
            tenantSlug = "test",
            authorities = emptySet(),
        )
        val context = SecurityContextHolder.createEmptyContext()
        context.authentication =
            UsernamePasswordAuthenticationToken(user, null, emptyList())
        SecurityContextHolder.setContext(context)

        try {
            val revision = AuditRevision()
            listener.newRevision(revision)

            assertEquals(user.userId, revision.actorId)
        } finally {
            SecurityContextHolder.clearContext()
        }
    }

    @Test
    fun `leaves actor empty without authentication`() {
        SecurityContextHolder.clearContext()

        val revision = AuditRevision()
        listener.newRevision(revision)

        assertNull(revision.actorId)
    }
}
