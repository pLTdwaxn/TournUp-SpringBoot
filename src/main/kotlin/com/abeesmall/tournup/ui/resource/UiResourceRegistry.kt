package com.abeesmall.tournup.ui.resource

import com.abeesmall.tournup.dto.ui.UiResourceIdentifier
import com.abeesmall.tournup.dto.ui.UiResourceOperation
import com.abeesmall.tournup.dto.ui.UiResourceRelationship
import com.abeesmall.tournup.ui.UiApplication
import com.abeesmall.tournup.ui.UiFieldDefinition
import com.abeesmall.tournup.ui.UiViewDefinition
import com.abeesmall.tournup.ui.UiOperation
import org.springframework.security.core.Authentication
import org.springframework.stereotype.Component

data class UiResourceRegistration(
    val resourceClass: Class<*>,
    val audited: Boolean,
    val applications: Set<UiApplication>,
    val label: String,
    val displayField: String?,
    val fields: Map<String, UiFieldDefinition>,
    val identifier: UiResourceIdentifier,
    val relationships: List<UiResourceRelationship>,
    val operations: List<UiResourceOperation>,
    val views: Map<UiOperation, UiViewDefinition> = emptyMap(),
    val canRead: (Authentication) -> Boolean,
    val canWrite: (Authentication) -> Boolean,
) {
    val pluralName: String
        get() = requireNotNull(resourceClass.getAnnotation(UiResource::class.java)) {
            "Missing @UiResource on ${resourceClass.name}"
        }.pluralName
}

@Component
class UiResourceRegistry(registrations: List<UiResourceRegistration>) {
    // Registration is explicit: annotating another DTO alone never exposes it.
    val resources: Map<String, UiResourceRegistration> = buildMap {
        val pluralNames = mutableSetOf<String>()
        for (registration in registrations) {
            val annotation = registration.resourceClass.getAnnotation(UiResource::class.java)
            requireNotNull(annotation) { "Missing @UiResource on ${registration.resourceClass.name}" }
            val key = annotation.key
            require(key.matches(Regex("[a-z][a-z0-9-]*"))) { "Invalid UI resource key: $key" }
            require(key !in this) { "Duplicate UI resource key: $key" }
            val pluralName = annotation.pluralName
            require(pluralName.matches(Regex("[a-z][a-z0-9]*(?:-[a-z0-9]+)*"))) {
                "Invalid UI resource plural name: $pluralName"
            }
            require(pluralNames.add(pluralName)) { "Duplicate UI resource plural name: $pluralName" }
            put(key, registration)
        }
    }.toSortedMap()

    init {
        for ((key, registration) in resources) {
            val operationIds = registration.operations.map { it.id }
            require(operationIds.distinct().size == operationIds.size) { "Duplicate operation ID in $key" }
            for (view in registration.views.values) {
                require(view.fieldIds.all { it in registration.fields }) { "Unknown view field in $key" }
                require(view.commands.all { it.operationId in operationIds }) { "Unknown command operation in $key" }
            }
            require(registration.relationships.all { it.resource in resources }) {
                "Unknown relationship resource in $key"
            }
            for (field in registration.fields.values) {
                val reference = field.reference ?: continue
                val target = requireNotNull(resources[reference.resource]) { "Unknown reference resource in $key" }
                require(target.operations.any { it.id == reference.lookupOperation }) { "Unknown reference lookup in $key" }
                require(target.operations.any { it.id == reference.resolveOperation }) { "Unknown reference resolver in $key" }
            }
        }
    }
}
