package com.abeesmall.tournup.ui.resource

@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.RUNTIME)
annotation class UiResource(
    val key: String,
    val pluralName: String,
)
