package com.abeesmall.tournup.validation

import jakarta.validation.Constraint
import jakarta.validation.OverridesAttribute
import jakarta.validation.Payload
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Pattern
import jakarta.validation.constraints.Size
import kotlin.reflect.KClass

const val RESOURCE_NAME_MAX_LENGTH = 255
const val RESOURCE_SLUG_MAX_LENGTH = 255
const val RESOURCE_SLUG_PATTERN = "^[a-z0-9]+(?:-[a-z0-9]+)*$"

@Target(AnnotationTarget.FIELD, AnnotationTarget.VALUE_PARAMETER, AnnotationTarget.ANNOTATION_CLASS)
@Retention(AnnotationRetention.RUNTIME)
@Constraint(validatedBy = [])
@NotBlank(message = "Name is required")
@Size(max = RESOURCE_NAME_MAX_LENGTH, message = "Name must be at most {max} characters")
annotation class ResourceName(
    @get:OverridesAttribute(constraint = Size::class, name = "max")
    val max: Int = RESOURCE_NAME_MAX_LENGTH,
    val message: String = "Invalid name",
    val groups: Array<KClass<*>> = [],
    val payload: Array<KClass<out Payload>> = [],
)

@Target(AnnotationTarget.FIELD, AnnotationTarget.VALUE_PARAMETER, AnnotationTarget.ANNOTATION_CLASS)
@Retention(AnnotationRetention.RUNTIME)
@Constraint(validatedBy = [])
@NotBlank(message = "Slug is required")
@Size(max = RESOURCE_SLUG_MAX_LENGTH, message = "Slug must be at most {max} characters")
@Pattern(regexp = RESOURCE_SLUG_PATTERN, message = "Use lowercase letters and digits separated by single hyphens")
annotation class ResourceSlug(
    @get:OverridesAttribute(constraint = Size::class, name = "max")
    val max: Int = RESOURCE_SLUG_MAX_LENGTH,
    val message: String = "Invalid slug",
    val groups: Array<KClass<*>> = [],
    val payload: Array<KClass<out Payload>> = [],
)
