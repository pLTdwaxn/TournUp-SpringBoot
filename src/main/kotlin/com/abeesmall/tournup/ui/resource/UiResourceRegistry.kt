package com.abeesmall.tournup.ui.resource

import com.abeesmall.tournup.dto.ui.UiResourceIdentifier
import com.abeesmall.tournup.dto.ui.UiResourceOperation
import com.abeesmall.tournup.dto.ui.UiResourceRelationship
import com.abeesmall.tournup.ui.UiApplication
import com.abeesmall.tournup.ui.UiField
import org.springframework.security.core.Authentication
import org.springframework.stereotype.Component

data class UiResourceRegistration(
    val modelClass: Class<*>,
    val applications: Set<UiApplication>,
    val label: String,
    val displayField: String?,
    val fields: Map<String, UiField>,
    val identifier: UiResourceIdentifier,
    val relationships: List<UiResourceRelationship>,
    val operations: List<UiResourceOperation>,
    val canRead: (Authentication) -> Boolean,
    val canWrite: (Authentication) -> Boolean,
)

@Component
class UiResourceRegistry(registrations: List<UiResourceRegistration>) {
    // Registration is explicit: annotating another entity alone never exposes it.
    val resources: Map<String, UiResourceRegistration> = buildMap {
        for (registration in registrations) {
            val annotation = registration.modelClass.getAnnotation(UiResource::class.java)
            requireNotNull(annotation) { "Missing @UiResource on ${registration.modelClass.name}" }
            val key = annotation.key
            require(key.matches(Regex("[a-z][a-z0-9-]*"))) { "Invalid UI resource key: $key" }
            require(key !in this) { "Duplicate UI resource key: $key" }
            put(key, registration)
        }
    }.toSortedMap()

    init {
        for ((key, registration) in resources) {
            require(registration.relationships.all { it.resource in resources }) {
                "Unknown relationship resource in $key"
            }
        }
    }
}
