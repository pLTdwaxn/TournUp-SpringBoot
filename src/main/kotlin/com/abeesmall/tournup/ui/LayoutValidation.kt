package com.abeesmall.tournup.ui

private fun layoutFieldIds(definition: LayoutDefinition): List<String> =
    when (definition.layout.type) {
        UiLayoutType.FORM, UiLayoutType.DETAIL ->
            definition.layout.sections.flatMap { it.fields }

        UiLayoutType.TABLE ->
            definition.layout.columns
    }

fun unknownLayoutFieldIds(
    definition: LayoutDefinition,
    availableFieldIds: Set<String>,
): Set<String> {
    val placedFieldIds = layoutFieldIds(definition)
        .toSet()

    return (placedFieldIds + definition.fields.keys) - availableFieldIds
}

fun incompatibleWidgetFieldIds(
    definition: LayoutDefinition,
    fieldTypes: Map<String, UiDataType>,
): Set<String> =
    definition.fields
        .filter { (fieldId, presentation) ->
            val dataType = fieldTypes[fieldId] ?: return@filter false

            when (definition.layout.type) {
                UiLayoutType.FORM -> when (dataType) {
                    UiDataType.STRING ->
                        presentation.widget !in setOf(
                            UiWidget.TEXT,
                            UiWidget.TEXTAREA,
                        )

                    UiDataType.ENUM ->
                        presentation.widget != UiWidget.SELECT
                }

                UiLayoutType.TABLE, UiLayoutType.DETAIL ->
                    presentation.widget != UiWidget.DISPLAY
            }
        }
        .keys

fun omittedRequiredFieldIds(
    definition: LayoutDefinition,
    requiredWithoutDefaults: Set<String>,
): Set<String> {
    if (definition.layout.type != UiLayoutType.FORM) {
        return emptySet()
    }

    val placedFieldIds = layoutFieldIds(definition)
        .toSet()

    return requiredWithoutDefaults - placedFieldIds
}

fun missingPresentationFieldIds(
    definition: LayoutDefinition
): Set<String> {
    val placedFieldIds = layoutFieldIds(definition)
        .toSet()

    return placedFieldIds - definition.fields.keys
}

fun duplicatePlacedFieldIds(definition: LayoutDefinition): Set<String> =
    layoutFieldIds(definition)
        .groupingBy { it }
        .eachCount()
        .filterValues { it > 1 }
        .keys

data class LayoutValidationReport(
    val unknownFields: Set<String>,
    val incompatibleWidgets: Set<String>,
    val omittedRequiredFields: Set<String>,
    val missingPresentations: Set<String>,
    val duplicatePlacements: Set<String>,
) {
    val isValid: Boolean
        get() = listOf(
            unknownFields,
            incompatibleWidgets,
            omittedRequiredFields,
            missingPresentations,
            duplicatePlacements,
        ).all { it.isEmpty() }
}

fun validateLayoutDefinition(
    definition: LayoutDefinition,
    fieldTypes: Map<String, UiDataType>,
    requiredWithoutDefaults: Set<String>,
): LayoutValidationReport =
    LayoutValidationReport(
        unknownFields = unknownLayoutFieldIds(definition, fieldTypes.keys),
        incompatibleWidgets = incompatibleWidgetFieldIds(definition, fieldTypes),
        omittedRequiredFields = omittedRequiredFieldIds(definition, requiredWithoutDefaults),
        missingPresentations = missingPresentationFieldIds(definition),
        duplicatePlacements = duplicatePlacedFieldIds(definition),
    )
