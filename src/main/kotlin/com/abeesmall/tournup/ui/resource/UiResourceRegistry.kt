package com.abeesmall.tournup.ui.resource

import com.abeesmall.tournup.dto.ui.UiResourceIdentifier
import com.abeesmall.tournup.dto.ui.UiResourceOperation
import com.abeesmall.tournup.dto.ui.UiResourceRelationship
import com.abeesmall.tournup.resource.ResourceAction
import com.abeesmall.tournup.resource.ResourceDefinition
import com.abeesmall.tournup.resource.ResourceRegistry
import com.abeesmall.tournup.ui.UiApplication
import com.abeesmall.tournup.ui.UiCommandMethod
import com.abeesmall.tournup.ui.UiFieldPresentation
import com.abeesmall.tournup.ui.UiOperation
import com.abeesmall.tournup.ui.UiViewDefinition
import org.springframework.stereotype.Component

data class UiResourceRegistration(
    val definition: ResourceDefinition,
    val audited: Boolean,
    val applications: Set<UiApplication>,
    val label: String,
    val displayField: String?,
    val presentation: Map<String, UiFieldPresentation>,
    val identifier: UiResourceIdentifier,
    val relationships: List<UiResourceRelationship>,
    val operations: List<UiResourceOperation>,
    val views: Map<UiOperation, UiViewDefinition> = emptyMap(),
    val publishedViews: Set<UiOperation> = emptySet(),
) {
    fun supports(operation: UiResourceOperation): Boolean = when (operation.operation) {
        UiOperation.LIST -> ResourceAction.LIST
        UiOperation.READ -> ResourceAction.READ
        UiOperation.CREATE -> if (operation.method == UiCommandMethod.GET) ResourceAction.CREATE_FORM else ResourceAction.CREATE
        UiOperation.UPDATE -> if (operation.method == UiCommandMethod.GET) ResourceAction.UPDATE_FORM else ResourceAction.UPDATE
    } in definition.actions
}

@Component
class UiResourceRegistry(registrations: List<UiResourceRegistration>, registry: ResourceRegistry) {
    // Registration is explicit: annotating another DTO alone never exposes it.
    val resources: Map<String, UiResourceRegistration> = buildMap {
        for (registration in registrations) {
            val definition = registration.definition
            val key = definition.key
            require(registry.resources[definition.pluralName]?.definition === definition) {
                "UI resource $key must reference its registered operation definition"
            }
            require(key !in this) { "Duplicate UI registration: $key" }
            put(key, registration)
        }
    }.toSortedMap()

    init {
        for ((key, registration) in resources) {
            val operationIds = registration.operations.map { it.id }
            require(operationIds.distinct().size == operationIds.size) { "Duplicate operation ID in $key" }
            for (view in registration.views.values) {
                require(view.fieldIds.all { it in registration.presentation }) { "Unknown view field in $key" }
                require(view.commands.all { it.operationId in operationIds }) { "Unknown command operation in $key" }
            }
            require(registration.relationships.all { it.resource in resources }) {
                "Unknown relationship resource in $key"
            }
            for (field in registration.presentation.values) {
                val reference = field.reference ?: continue
                val target = requireNotNull(resources[reference.resource]) { "Unknown reference resource in $key" }
                require(target.operations.any { it.id == reference.lookupOperation }) { "Unknown reference lookup in $key" }
                require(target.operations.any { it.id == reference.resolveOperation }) { "Unknown reference resolver in $key" }
            }
        }
    }
}
