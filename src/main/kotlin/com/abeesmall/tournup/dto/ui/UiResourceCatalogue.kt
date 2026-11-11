package com.abeesmall.tournup.dto.ui

import com.abeesmall.tournup.ui.UiCommandMethod
import com.abeesmall.tournup.ui.UiDataType
import com.abeesmall.tournup.ui.UiOperation
import com.abeesmall.tournup.ui.UiOption
import com.abeesmall.tournup.ui.UiReference
import java.util.UUID

data class UiResourceCatalogue(
    val contractVersion: Int,
    val application: String,
    val scope: UiCatalogueScope,
    val resources: List<UiResourceDescriptor>,
)

data class UiCatalogueScope(val tenantId: UUID, val userId: UUID)

data class UiResourceDescriptor(
    val key: String,
    val pluralName: String,
    val label: String,
    val displayField: String?,
    val fields: Map<String, UiResourceField>,
    val identifier: UiResourceIdentifier,
    val relationships: List<UiResourceRelationship>,
    val operations: List<UiResourceOperation>,
    val canReadLastAudit: Boolean,
)

// Presentation widgets and behaviours remain in each operation's resolved manifest.
data class UiResourceField(
    val label: String,
    val dataType: UiDataType,
    val required: Boolean,
    val options: List<UiOption>,
    val maxLength: Int?,
    val pattern: String?,
    val reference: UiReference?,
)

data class UiResourceIdentifier(
    val field: String,
    val type: String,
    val slugField: String? = null,
    val slugContext: List<String> = emptyList(),
)

data class UiResourceRelationship(
    val field: String,
    val resource: String,
    val label: String,
    val required: Boolean,
)

// href may be a template. requiredContext identifies parent context, not the record ID.
data class UiResourceOperation(
    val id: String,
    val operation: UiOperation,
    val method: UiCommandMethod,
    val href: String,
    val requiredContext: List<String> = emptyList(),
)
