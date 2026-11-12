package com.abeesmall.tournup.resource

import kotlin.reflect.KClass
import kotlin.reflect.full.memberProperties
import kotlin.reflect.full.primaryConstructor
import kotlin.reflect.jvm.javaField

enum class WriteOperation { CREATE, UPDATE }

@Target(AnnotationTarget.FIELD)
@Retention(AnnotationRetention.RUNTIME)
annotation class WriteField(val writableOn: Array<WriteOperation>)

/** Request DTOs own value constraints; this contract selects their operation and initial values. */
class WriteContract<T : Any>(
    val requestType: KClass<T>,
    val operation: WriteOperation,
    val initialValues: (() -> T)? = null,
) {
    private val parameters = requireNotNull(requestType.primaryConstructor).parameters.associateBy { it.name }
    private val properties = requestType.memberProperties.filter { it.name in parameters }.associateBy { it.name }

    fun writable(field: String): Boolean = properties[field]?.let {
        val operations = it.javaField?.getAnnotation(WriteField::class.java)?.writableOn
        operations == null || operation in operations
    } ?: false

    fun defaultValue(field: String): String? {
        if (parameters[field]?.isOptional != true) return null
        val initial = initialValues?.invoke() ?: return null
        val value = properties.getValue(field).get(initial)
        return if (value is Enum<*>) value.name else value?.toString()
    }

    /** Call with persisted values before mutating the entity; omitted state must fail closed. */
    fun requireUnchanged(request: T, original: Map<String, Any?>, failure: (String) -> RuntimeException) {
        for ((name, property) in properties) {
            if (writable(name)) continue
            require(name in original) { "Missing original value for immutable field $name" }
            if (property.get(request) != original[name]) {
                throw failure(name)
            }
        }
    }
}
