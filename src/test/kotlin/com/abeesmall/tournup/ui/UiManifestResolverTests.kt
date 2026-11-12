package com.abeesmall.tournup.ui

import com.abeesmall.tournup.dto.ui.UiResourceIdentifier
import com.abeesmall.tournup.dto.ui.UiResourceOperation
import com.abeesmall.tournup.manifest.season.seasonResourceDefinition
import com.abeesmall.tournup.support.UiTestSupport
import com.abeesmall.tournup.ui.resource.Resource
import com.abeesmall.tournup.ui.resource.ResourceAction
import com.abeesmall.tournup.ui.resource.UiResourceRegistration
import com.abeesmall.tournup.validation.WriteContract
import com.abeesmall.tournup.validation.WriteField
import com.abeesmall.tournup.validation.WriteOperation
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.RegisterExtension

class UiManifestResolverTests {
    @RegisterExtension @JvmField val ui = UiTestSupport()

    @Test
    fun `commands use the operation definition shared with the catalogue`() {
        val definition = seasonResourceDefinition()
        val changed = definition.copy(operations = definition.operations.map {
            if (it.id == "update") it.copy(href = "/api/revised-seasons/{id}/") else it
        })
        val manifest = ui.manifests.resolve(changed,
            UiManifestContext(UiApplication.TOURNAMENT_MANAGER, UiOperation.UPDATE,
                setOf(UiOperation.UPDATE), recordId = "record-123"))
        assertEquals("/api/revised-seasons/record-123/", manifest.commands.single().href)
        assertEquals(UiCommandMethod.PUT, manifest.commands.single().method)
    }

    @Resource(key = "sample", pluralName = "samples")
    private data class Sample(
        val parentId: String,
        val name: String,
        val unmanaged: String,
    )

    private data class SampleWrite(
        @field:WriteField(writableOn = [WriteOperation.CREATE]) val parentId: String,
        val name: String,
    )

    private fun definition(operation: UiOperation): UiResourceRegistration = UiResourceRegistration(

            resourceClass = Sample::class.java,
            actions = ResourceAction.entries.toSet(),
            create = WriteContract(SampleWrite::class, WriteOperation.CREATE),
            update = WriteContract(SampleWrite::class, WriteOperation.UPDATE),
            canRead = { true }, canWrite = { true },
        audited = false,
        label = "Samples",
        displayField = "name",
        identifier = UiResourceIdentifier("id", "uuid"),
        relationships = emptyList(),
        operations = listOf(UiResourceOperation("save", operation, UiCommandMethod.POST, "/api/samples/")),
        applications = setOf(UiApplication.TOURNAMENT_MANAGER),
        presentation = mapOf(
            "parentId" to UiFieldPresentation("Parent", UiDataType.STRING),
            "name" to UiFieldPresentation("Name", UiDataType.STRING),
            "unmanaged" to UiFieldPresentation("Unmanaged", UiDataType.STRING),
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
    fun `write contract permits parent selection only during creation`() {
        for (operation in listOf(UiOperation.CREATE, UiOperation.UPDATE)) {
            val result = ui.manifests.resolve(definition(operation),
                UiManifestContext(UiApplication.TOURNAMENT_MANAGER, operation, setOf(operation)))
            assertEquals("sample", result.resource)
            assertEquals(operation == UiOperation.CREATE, result.fields.getValue("parentId").editable)
            assertTrue(result.fields.getValue("name").editable)
            assertFalse(result.fields.getValue("unmanaged").editable)
            assertEquals(1, result.commands.size)
        }
    }

    @Test
    fun `write contract cannot grant writes without user permission`() {
        val result = ui.manifests.resolve(definition(UiOperation.CREATE),
            UiManifestContext(UiApplication.TOURNAMENT_MANAGER, UiOperation.CREATE))
        assertTrue(result.fields.values.all { !it.editable && it.behaviours.isEmpty() })
        assertTrue(result.commands.isEmpty())
    }
}
