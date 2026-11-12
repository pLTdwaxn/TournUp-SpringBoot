package com.abeesmall.tournup.validation

import jakarta.validation.Validator
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotEmpty
import jakarta.validation.constraints.NotNull
import jakarta.validation.constraints.Pattern
import jakarta.validation.constraints.Size
import jakarta.validation.groups.Default
import jakarta.validation.metadata.ConstraintDescriptor
import java.util.concurrent.ConcurrentHashMap
import kotlin.reflect.KClass
import kotlin.reflect.full.primaryConstructor
import org.springframework.stereotype.Component

data class WriteFieldMetadata(
    // A value is required after binding/defaults. This is distinct from JSON property presence.
    val required: Boolean,
    val acceptsOmission: Boolean,
    val hasDefault: Boolean,
    val maxLength: Int?,
    val pattern: String?,
)

@Component
class WriteContractMetadata(private val validator: Validator) {
    private val cache = ConcurrentHashMap<KClass<*>, Map<String, WriteFieldMetadata>>()

    fun read(type: KClass<*>): Map<String, WriteFieldMetadata> = cache.computeIfAbsent(type) {
        val bean = validator.getConstraintsForClass(type.java)
        requireNotNull(type.primaryConstructor) { "Write DTO needs a primary constructor: $type" }
            .parameters.associate { parameter ->
                val name = requireNotNull(parameter.name)
                val constraints = bean.getConstraintsForProperty(name)?.findConstraints()
                    ?.unorderedAndMatchingGroups(Default::class.java)?.constraintDescriptors
                    .orEmpty().flatMap { flatten(it) }
                val required = !parameter.type.isMarkedNullable || constraints.any {
                    it.annotation is NotNull || it.annotation is NotBlank || it.annotation is NotEmpty
                }
                val patterns = constraints.mapNotNull { it.annotation as? Pattern }
                // Export only the deliberately supported, portable slug pattern. Other rules
                // still execute on the server and can gain explicit client translators later.
                val pattern = patterns.singleOrNull()?.takeIf {
                    it.flags.isEmpty() && it.regexp == RESOURCE_SLUG_PATTERN
                }?.regexp
                name to WriteFieldMetadata(
                    required = required,
                    acceptsOmission = parameter.isOptional || parameter.type.isMarkedNullable,
                    hasDefault = parameter.isOptional,
                    maxLength = if (parameter.type.classifier == String::class) {
                        constraints.mapNotNull { (it.annotation as? Size)?.max }.minOrNull()
                    } else null,
                    pattern = pattern,
                )
            }
    }

    private fun flatten(constraint: ConstraintDescriptor<*>): List<ConstraintDescriptor<*>> =
        listOf(constraint) + constraint.composingConstraints.flatMap { flatten(it) }
}
