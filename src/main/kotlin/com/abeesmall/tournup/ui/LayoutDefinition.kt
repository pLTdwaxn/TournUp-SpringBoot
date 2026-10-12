package com.abeesmall.tournup.ui

data class LayoutDefinition(
    val title: String,
    val layout: UiLayout,
    val fields: Map<String, FieldPresentation>,
)

data class FieldPresentation(
    val label: String,
    val widget: UiWidget,
)
