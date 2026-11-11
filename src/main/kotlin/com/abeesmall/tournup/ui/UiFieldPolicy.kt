package com.abeesmall.tournup.ui

@Target(AnnotationTarget.FIELD)
@Retention(AnnotationRetention.RUNTIME)
annotation class UiFieldPolicy(val writableOn: Array<UiOperation>)

object UiFieldPolicies {
    fun writableOn(resourceClass: Class<*>, property: String): Set<UiOperation> {
        val field = resourceClass.getDeclaredField(property)
        return field.getAnnotation(UiFieldPolicy::class.java)?.writableOn?.toSet() ?: emptySet()
    }
}
