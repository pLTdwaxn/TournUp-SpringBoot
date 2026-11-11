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
) {
    val pluralName: String
        get() = requireNotNull(modelClass.getAnnotation(UiResource::class.java)) {
            "Missing @UiResource on ${modelClass.name}"
        }.pluralName
}

@Component
class UiResourceRegistry(registrations: List<UiResourceRegistration>) {
    // Registration is explicit: annotating another entity alone never exposes it.
    val resources: Map<String, UiResourceRegistration> = buildMap {
        val pluralNames = mutableSetOf<String>()
        for (registration in registrations) {
            val annotation = registration.modelClass.getAnnotation(UiResource::class.java)
            requireNotNull(annotation) { "Missing @UiResource on ${registration.modelClass.name}" }
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
            require(registration.relationships.all { it.resource in resources }) {
                "Unknown relationship resource in $key"
            }
        }
    }
}
