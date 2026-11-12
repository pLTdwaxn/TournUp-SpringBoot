package com.abeesmall.tournup.ui

import com.abeesmall.tournup.resource.WriteContract
import com.abeesmall.tournup.validation.WriteContractMetadata
import org.springframework.stereotype.Component

data class UiFieldPresentation(
    val label: String,
    val dataType: UiDataType,
    val reference: UiReference? = null,
    val options: List<UiOption> = emptyList(),
)

@Component
class UiConstraintProjector(private val metadata: WriteContractMetadata) {
    fun project(
        contract: WriteContract<*>?,
        presentation: Map<String, UiFieldPresentation>,
    ): Map<String, UiFieldDefinition> {
        val fields = contract?.let { metadata.read(it.requestType) }.orEmpty()
        return presentation.mapValues { (name, ui) ->
            val constraints = fields[name]
            UiFieldDefinition(
                label = ui.label, dataType = ui.dataType,
                required = constraints?.required ?: false,
                maxLength = constraints?.maxLength, pattern = constraints?.pattern,
                reference = ui.reference, options = ui.options,
                defaultValue = contract?.defaultValue(name),
            )
        }
    }
}
