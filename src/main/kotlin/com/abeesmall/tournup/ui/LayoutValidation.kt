package com.abeesmall.tournup.ui

fun unknownLayoutFieldIds(
    definition: LayoutDefinition,
    availableFieldIds: Set<String>,
): Set<String> {
    val placedFieldIds = definition.layout.sections
        .flatMap { it.fields }
        .toSet()

    return (placedFieldIds + definition.fields.keys) - availableFieldIds
}

fun incompatibleWidgetFieldIds(
    definition: LayoutDefinition,
    fieldTypes: Map<String, UiDataType>,
): Set<String> =
    definition.fields
        .filter { (fieldId, presentation) ->
            when (fieldTypes[fieldId]) {
                UiDataType.STRING -> presentation.widget == UiWidget.SELECT
                UiDataType.ENUM -> presentation.widget != UiWidget.SELECT
                null -> false
            }
        }
        .keys

fun omittedRequiredFieldIds(
    definition: LayoutDefinition,
    requiredWithoutDefaults: Set<String>,
): Set<String> {
    val placedFieldIds = definition.layout.sections
        .flatMap { it.fields }
        .toSet()

    return requiredWithoutDefaults - placedFieldIds
}

fun missingPresentationFieldIds(
    definition: LayoutDefinition
): Set<String> {
    val placedFieldIds = definition.layout.sections
        .flatMap { it.fields }
        .toSet()

    return placedFieldIds - definition.fields.keys
}

fun duplicatePlacedFieldIds(definition: LayoutDefinition): Set<String> =
    definition.layout.sections
        .flatMap { it.fields }
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
