package com.abeesmall.tournup.resource

import org.springframework.stereotype.Component

@Component
class ResourceRegistry(handlers: List<ResourceHandler<*, *, *>>) {
    val resources: Map<String, ResourceHandler<*, *, *>> = buildMap {
        val keys = mutableSetOf<String>()
        for (handler in handlers) {
            val definition = handler.definition
            require(definition.key.matches(Regex("[a-z][a-z0-9-]*"))) { "Invalid resource key" }
            require(definition.pluralName.matches(Regex("[a-z][a-z0-9]*(?:-[a-z0-9]+)*"))) {
                "Invalid resource plural name"
            }
            require(keys.add(definition.key)) { "Duplicate resource key: ${definition.key}" }
            require(definition.pluralName !in this) { "Duplicate resource plural name" }
            put(definition.pluralName, handler)
        }
    }
}
