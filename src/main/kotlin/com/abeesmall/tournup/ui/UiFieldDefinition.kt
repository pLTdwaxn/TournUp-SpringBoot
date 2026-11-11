package com.abeesmall.tournup.ui

data class UiFieldDefinition(
    val label: String,
    val dataType: UiDataType,
    val required: Boolean,
    val maxLength: Int? = null,
    val pattern: String? = null,
    val reference: UiReference? = null,
    val options: List<UiOption> = emptyList(),
    val defaultValue: String? = null,
) {
    val allowedValues: List<String>
        get() = options.map { it.value }

    fun toUiField(
        widget: UiWidget,
        editable: Boolean = false,
        behaviours: List<UiBehaviour> = emptyList(),
    ): UiField = UiField(
        label = label,
        dataType = dataType,
        widget = widget,
        required = required,
        editable = editable,
        behaviours = behaviours,
        maxLength = maxLength,
        pattern = pattern,
        reference = reference,
        options = options,
    )
}

