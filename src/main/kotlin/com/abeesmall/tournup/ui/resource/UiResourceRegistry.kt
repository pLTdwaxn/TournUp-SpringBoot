package com.abeesmall.tournup.ui.resource

import com.abeesmall.tournup.dto.ui.UiResourceIdentifier
import com.abeesmall.tournup.dto.ui.UiResourceOperation
import com.abeesmall.tournup.dto.ui.UiResourceRelationship
import com.abeesmall.tournup.ui.UiApplication
import com.abeesmall.tournup.ui.UiCommandMethod
import com.abeesmall.tournup.ui.UiFieldPresentation
import com.abeesmall.tournup.ui.UiOperation
import com.abeesmall.tournup.ui.UiViewDefinition
import com.abeesmall.tournup.validation.WriteContract
import org.springframework.security.core.Authentication
import org.springframework.stereotype.Component

data class UiResourceRegistration(
    val resourceClass: Class<*>,
    val create: WriteContract<*>? = null,
    val update: WriteContract<*>? = null,
    val actions: Set<ResourceAction> = emptySet(),
    val canRead: (Authentication) -> Boolean,
    val canWrite: (Authentication) -> Boolean,
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
    private val identity get() = requireNotNull(resourceClass.getAnnotation(Resource::class.java)) {
        "Missing @Resource on ${resourceClass.name}"
    }
    val key get() = identity.key
    val pluralName get() = identity.pluralName

    fun supports(operation: UiResourceOperation): Boolean = when (operation.operation) {
        UiOperation.LIST -> ResourceAction.LIST
        UiOperation.READ -> ResourceAction.READ
        UiOperation.CREATE -> if (operation.method == UiCommandMethod.GET) ResourceAction.CREATE_FORM else ResourceAction.CREATE
        UiOperation.UPDATE -> if (operation.method == UiCommandMethod.GET) ResourceAction.UPDATE_FORM else ResourceAction.UPDATE
    } in actions
}

@Component
class UiResourceRegistry(registrations: List<UiResourceRegistration>) {
    // Registration is explicit: annotating another DTO alone never exposes it.
    val resources: Map<String, UiResourceRegistration> = buildMap<String, UiResourceRegistration> {
        for (registration in registrations) {
            val definition = registration
            val key = definition.key
            require(key.matches(Regex("[a-z][a-z0-9-]*"))) { "Invalid resource key" }
            require(definition.pluralName.matches(Regex("[a-z][a-z0-9]*(?:-[a-z0-9]+)*"))) { "Invalid plural name" }
            require(values.none { it.pluralName == definition.pluralName }) { "Duplicate plural name" }
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

@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.RUNTIME)
annotation class Resource(val key: String, val pluralName: String)

enum class ResourceAction(val write: Boolean) {
    LIST(false), READ(false), CREATE_FORM(true), UPDATE_FORM(true), CREATE(true), UPDATE(true),
}
