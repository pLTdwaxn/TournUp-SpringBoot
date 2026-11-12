package com.abeesmall.tournup.validation

import com.abeesmall.tournup.model.ResourceIdentifiers
import jakarta.validation.Constraint
import jakarta.validation.ConstraintValidator
import jakarta.validation.ConstraintValidatorContext
import jakarta.validation.Payload
import kotlin.reflect.KClass

@Target(AnnotationTarget.FIELD, AnnotationTarget.VALUE_PARAMETER)
@Retention(AnnotationRetention.RUNTIME)
@Constraint(validatedBy = [ResourceLocatorValidator::class])
annotation class ResourceLocator(
    val message: String = "Expected a slug-nanoID resource identifier",
    val groups: Array<KClass<*>> = [],
    val payload: Array<KClass<out Payload>> = [],
)

class ResourceLocatorValidator : ConstraintValidator<ResourceLocator, String> {
    override fun isValid(value: String?, context: ConstraintValidatorContext): Boolean =
        value == null || ResourceIdentifiers.nanoId(value) != null
}
