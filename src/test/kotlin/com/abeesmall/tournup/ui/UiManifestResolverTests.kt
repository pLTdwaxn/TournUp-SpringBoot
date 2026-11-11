package com.abeesmall.tournup.ui

import com.abeesmall.tournup.ui.resource.UiResource
import com.abeesmall.tournup.ui.resource.UiResourceRegistration
import com.abeesmall.tournup.dto.ui.UiResourceIdentifier
import com.abeesmall.tournup.dto.ui.UiResourceOperation
import com.abeesmall.tournup.manifest.season.seasonResourceDefinition
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import org.junit.jupiter.api.Test

class UiManifestResolverTests {
    @Test
    fun `commands use the operation definition shared with the catalogue`() {
        val definition = seasonResourceDefinition()
        val changed = definition.copy(operations = definition.operations.map {
            if (it.id == "update") it.copy(href = "/api/revised-seasons/{id}/") else it
        })
        val manifest = UiManifestResolver.resolve(changed,
            UiManifestContext(UiApplication.TOURNAMENT_MANAGER, UiOperation.UPDATE,
                setOf(UiOperation.UPDATE), recordId = "record-123"))
        assertEquals("/api/revised-seasons/record-123/", manifest.commands.single().href)
        assertEquals(UiCommandMethod.PUT, manifest.commands.single().method)
    }

    @UiResource(key = "sample", pluralName = "samples")
    private data class Sample(
        @field:UiFieldPolicy(writableOn = [UiOperation.CREATE]) val parentId: String,
        @field:UiFieldPolicy(writableOn = [UiOperation.CREATE, UiOperation.UPDATE]) val name: String,
        val unmanaged: String,
    )

    private fun definition(operation: UiOperation): UiResourceRegistration = UiResourceRegistration(
        resourceClass = Sample::class.java,
        audited = false,
        label = "Samples",
        displayField = "name",
        identifier = UiResourceIdentifier("id", "uuid"),
        relationships = emptyList(),
        operations = listOf(UiResourceOperation("save", operation, UiCommandMethod.POST, "/api/samples/")),
        canRead = { true },
        canWrite = { true },
        applications = setOf(UiApplication.TOURNAMENT_MANAGER),
        fields = mapOf(
            "parentId" to UiFieldDefinition("Parent", UiDataType.STRING, true),
            "name" to UiFieldDefinition("Name", UiDataType.STRING, true),
            "unmanaged" to UiFieldDefinition("Unmanaged", UiDataType.STRING, false),
        ),
        views = mapOf(operation to UiViewDefinition(
            id = "sample-form", title = "Sample",
            layout = UiLayout(UiLayoutType.FORM,
                listOf(UiSection("details", "Details", listOf("parentId", "name", "unmanaged")))),
            fieldIds = listOf("parentId", "name", "unmanaged"),
            behaviours = mapOf("name" to listOf(UiBehaviour(UiBehaviourType.SLUGIFY, listOf("parentId")))),
            commands = listOf(UiManifestCommand(operation, "save", "Save", "save")),
        )),
    )

    @Test
    fun `DTO policy permits parent selection only during creation`() {
        for (operation in listOf(UiOperation.CREATE, UiOperation.UPDATE)) {
            val result = UiManifestResolver.resolve(definition(operation),
                UiManifestContext(UiApplication.TOURNAMENT_MANAGER, operation, setOf(operation)))
            assertEquals("sample", result.resource)
            assertEquals(operation == UiOperation.CREATE, result.fields.getValue("parentId").editable)
            assertTrue(result.fields.getValue("name").editable)
            assertFalse(result.fields.getValue("unmanaged").editable)
            assertEquals(1, result.commands.size)
        }
    }

    @Test
    fun `DTO capabilities cannot grant writes without user permission`() {
        val result = UiManifestResolver.resolve(definition(UiOperation.CREATE),
            UiManifestContext(UiApplication.TOURNAMENT_MANAGER, UiOperation.CREATE))
        assertTrue(result.fields.values.all { !it.editable && it.behaviours.isEmpty() })
        assertTrue(result.commands.isEmpty())
    }
}
