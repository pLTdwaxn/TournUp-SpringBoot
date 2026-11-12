package com.abeesmall.tournup.resource

import org.springframework.security.core.Authentication

@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.RUNTIME)
annotation class Resource(val key: String, val pluralName: String)

enum class ResourceAction(val write: Boolean) {
    LIST(false), READ(false), CREATE_FORM(true), UPDATE_FORM(true), CREATE(true), UPDATE(true),
}

data class ResourceDefinition(
    val resourceClass: Class<*>,
    val create: WriteContract<*>? = null,
    val update: WriteContract<*>? = null,
    val actions: Set<ResourceAction> = emptySet(),
    val canRead: (Authentication) -> Boolean,
    val canWrite: (Authentication) -> Boolean,
) {
    private val identity: Resource
        get() = requireNotNull(resourceClass.getAnnotation(Resource::class.java)) {
            "Missing @Resource on ${resourceClass.name}"
        }
    val key: String get() = identity.key
    val pluralName: String get() = identity.pluralName
}
