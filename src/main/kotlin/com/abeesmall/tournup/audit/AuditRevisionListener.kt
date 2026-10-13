package com.abeesmall.tournup.audit

import com.abeesmall.tournup.auth.AuthenticatedUser
import org.hibernate.envers.RevisionListener
import org.springframework.security.core.context.SecurityContextHolder

class AuditRevisionListener : RevisionListener {
    override fun newRevision(revisionEntity: Any) {
        val revision = revisionEntity as AuditRevision
        val authentication =
            SecurityContextHolder.getContext().authentication

        val user = authentication
            ?.takeIf { it.isAuthenticated }
            ?.principal as? AuthenticatedUser

        revision.actorId = user?.userId
    }
}
