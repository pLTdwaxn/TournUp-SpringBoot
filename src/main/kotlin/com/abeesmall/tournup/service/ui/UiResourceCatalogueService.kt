package com.abeesmall.tournup.service.ui

import com.abeesmall.tournup.auth.AuthenticatedUser
import com.abeesmall.tournup.dto.ui.UiCatalogueScope
import com.abeesmall.tournup.dto.ui.UiResourceCatalogue
import com.abeesmall.tournup.dto.ui.UiResourceDescriptor
import com.abeesmall.tournup.dto.ui.UiResourceField
import com.abeesmall.tournup.security.canViewLastAudit
import com.abeesmall.tournup.ui.UiApplication
import com.abeesmall.tournup.ui.UiOperation
import com.abeesmall.tournup.ui.resource.UiResourceRegistry
import org.hibernate.envers.Audited
import org.springframework.security.core.Authentication
import org.springframework.stereotype.Service

@Service
class UiResourceCatalogueService(private val registry: UiResourceRegistry) {
    fun catalogue(
        application: UiApplication,
        authentication: Authentication,
        user: AuthenticatedUser,
    ): UiResourceCatalogue = UiResourceCatalogue(
        contractVersion = 1,
        application = application.id,
        scope = UiCatalogueScope(tenantId = user.tenantId, userId = user.userId),
        resources = registry.resources.entries
            .filter { (_, definition) ->
                application in definition.applications && definition.canRead(authentication)
            }
            .map { (key, definition) ->
                val canWrite = definition.canWrite(authentication)
                UiResourceDescriptor(
                    key = key,
                    label = definition.label,
                    displayField = definition.displayField,
                    fields = definition.fields.mapValues { (_, field) ->
                        UiResourceField(
                            label = field.label,
                            dataType = field.dataType,
                            required = field.required,
                            options = field.options,
                            maxLength = field.maxLength,
                            pattern = field.pattern,
                        )
                    },
                    identifier = definition.identifier,
                    relationships = definition.relationships,
                    operations = definition.operations.filter {
                        it.operation !in setOf(UiOperation.CREATE, UiOperation.UPDATE) || canWrite
                    },
                    canReadLastAudit = definition.modelClass.isAnnotationPresent(Audited::class.java) &&
                        canViewLastAudit(authentication),
                )
            },
    )
}
