package com.abeesmall.tournup.manifest.season

import com.abeesmall.tournup.manifest.season.seasonResourceDefinition
import com.abeesmall.tournup.ui.UiManifestContext
import com.abeesmall.tournup.ui.UiManifestResolver
import com.abeesmall.tournup.ui.UiOperation

import com.abeesmall.tournup.ui.UiApplication
import com.abeesmall.tournup.ui.UiWidget
import com.abeesmall.tournup.ui.resource.UiResourceConfiguration
import java.util.UUID
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import org.junit.jupiter.api.Test

class SeasonFieldCatalogueTests {
    private val create = UiManifestResolver.resolve(
        seasonResourceDefinition(),
        UiManifestContext(
            UiApplication.TOURNAMENT_MANAGER,
            UiOperation.CREATE,
            allowedWrites = setOf(UiOperation.CREATE),
        ),
    )
    private val edit = UiManifestResolver.resolve(
        seasonResourceDefinition(),
        UiManifestContext(
            UiApplication.TOURNAMENT_MANAGER,
            UiOperation.UPDATE,
            allowedWrites = setOf(UiOperation.UPDATE),
            recordId = UUID.randomUUID().toString(),
        ),
    )
    private val detail = UiManifestResolver.resolve(
        seasonResourceDefinition(),
        UiManifestContext(
            UiApplication.TOURNAMENT_MANAGER,
            UiOperation.READ,
            title = "2026 S1",
        ),
    )

    @Test
    fun `resource and operation manifests agree on shared field metadata`() {
        val registered = UiResourceConfiguration().seasonUiResource().fields
            .mapValues { (_, field) -> field.toUiField(UiWidget.DISPLAY) }
        for ((id, definition) in seasonFieldCatalogue()) {
            for (fields in listOf(create.fields, edit.fields, detail.fields, registered)) {
                val field = fields.getValue(id)
                assertEquals(definition.label, field.label)
                assertEquals(definition.dataType, field.dataType)
                assertEquals(definition.reference, field.reference)
            }
            for (fields in listOf(create.fields, edit.fields, registered)) {
                assertEquals(definition.maxLength, fields.getValue(id).maxLength)
                assertEquals(definition.pattern, fields.getValue(id).pattern)
                assertEquals(definition.required, fields.getValue(id).required)
            }
        }
    }

    @Test
    fun `operations preserve their own editing and suggestion rules`() {
        assertTrue(create.fields.values.all { it.editable })
        assertTrue(create.fields.getValue("name").behaviours.isNotEmpty())
        assertTrue(create.fields.getValue("slug").behaviours.isNotEmpty())
        assertFalse(edit.fields.getValue("tournamentId").editable)
        assertTrue(listOf("fromYear", "name", "slug").all { edit.fields.getValue(it).editable })
        assertTrue(edit.fields.values.all { it.behaviours.isEmpty() })
        assertTrue(detail.fields.values.all { !it.editable && it.widget == UiWidget.DISPLAY })
        assertTrue(detail.fields.values.all { it.behaviours.isEmpty() })
        assertEquals(listOf("name"), UiManifestResolver.resolve(
            seasonResourceDefinition(),
            UiManifestContext(
                UiApplication.TOURNAMENT_MANAGER,
                UiOperation.LIST,
                allowedWrites = if (false) setOf(UiOperation.CREATE) else emptySet(),
            ),
        ).layout.columns)
        assertEquals(setOf("fromYear", "name", "slug"), UiManifestResolver.resolve(
            seasonResourceDefinition(),
            UiManifestContext(
                UiApplication.TOURNAMENT_MANAGER,
                UiOperation.LIST,
                allowedWrites = if (false) setOf(UiOperation.CREATE) else emptySet(),
            ),
        ).fields.keys)
    }
}
