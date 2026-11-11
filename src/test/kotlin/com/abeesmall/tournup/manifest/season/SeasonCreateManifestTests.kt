package com.abeesmall.tournup.manifest.season

import com.abeesmall.tournup.dto.ui.UiRecordResponse

import com.abeesmall.tournup.manifest.season.seasonResourceDefinition
import com.abeesmall.tournup.ui.UiManifestContext
import com.abeesmall.tournup.ui.UiManifestResolver
import com.abeesmall.tournup.ui.UiOperation

import com.abeesmall.tournup.dto.season.SeasonWriteRequest
import com.abeesmall.tournup.ui.UiApplication
import java.util.UUID
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import org.junit.jupiter.api.Test
import tools.jackson.databind.json.JsonMapper

class SeasonCreateManifestTests {
    private val mapper = JsonMapper.builder().build()
    private val application = UiApplication.TOURNAMENT_MANAGER

    @Test
    fun `form defines an editable required reference before its dependent fields`() {
        val json = mapper.readTree(mapper.writeValueAsString(UiManifestResolver.resolve(
            seasonResourceDefinition(),
            UiManifestContext(
                application,
                UiOperation.CREATE,
                allowedWrites = setOf(UiOperation.CREATE),
            ),
        )))
        assertEquals("tournament-manager", json.path("application").stringValue())
        assertEquals("create", json.path("operation").stringValue())
        assertEquals(
            listOf("tournamentId", "fromYear", "name", "slug"),
            json.path("layout").path("sections").path(0).path("fields")
                .valueStream().map { it.stringValue() }.toList(),
        )
        val fields = json.path("fields")
        assertEquals(4, fields.size())
        for (field in listOf("tournamentId", "fromYear", "name", "slug")) {
            assertTrue(fields.path(field).path("required").booleanValue())
            assertTrue(fields.path(field).path("editable").booleanValue())
        }
        val reference = fields.path("tournamentId")
        assertEquals("reference", reference.path("dataType").stringValue())
        assertEquals("record-select", reference.path("widget").stringValue())
        assertEquals("tournament", reference.path("reference").path("resource").stringValue())
        assertEquals("lookup", reference.path("reference").path("lookupOperation").stringValue())
        assertEquals("read", reference.path("reference").path("resolveOperation").stringValue())
        assertEquals("integer", fields.path("fromYear").path("dataType").stringValue())
        assertEquals("number", fields.path("fromYear").path("widget").stringValue())
        assertEquals(255, fields.path("name").path("maxLength").intValue())
        assertEquals(255, fields.path("slug").path("maxLength").intValue())
        assertEquals("POST", json.path("commands").path(0).path("method").stringValue())
        assertEquals("/api/seasons/", json.path("commands").path(0).path("href").stringValue())
    }

    @Test
    fun `behaviours declare reference and year dependencies without the old sourceField contract`() {
        val json = mapper.readTree(mapper.writeValueAsString(UiManifestResolver.resolve(
            seasonResourceDefinition(),
            UiManifestContext(
                application,
                UiOperation.CREATE,
                allowedWrites = setOf(UiOperation.CREATE),
            ),
        )))
        val fields = json.path("fields")
        val name = fields.path("name").path("behaviours").path(0)
        val slug = fields.path("slug").path("behaviours").path(0)
        assertEquals("season-name-from-year", name.path("type").stringValue())
        assertEquals(
            listOf("tournamentId", "fromYear"),
            name.path("dependsOn").valueStream().map { it.stringValue() }.toList(),
        )
        assertEquals("naming-context", name.path("contextOperation").stringValue())
        assertEquals("slugify", slug.path("type").stringValue())
        assertEquals(listOf("name"), slug.path("dependsOn").valueStream().map { it.stringValue() }.toList())
        assertFalse(name.has("sourceField"))
        assertFalse(slug.has("sourceField"))
    }

    @Test
    fun `prefilled values and included reference records leave the field definition unchanged`() {
        val id = UUID.randomUUID()
        val empty = UiRecordResponse(UiManifestResolver.resolve(
            seasonResourceDefinition(),
            UiManifestContext(
                application,
                UiOperation.CREATE,
                allowedWrites = setOf(UiOperation.CREATE),
            ),
        ), item = SeasonWriteRequest("", "", null, null))
        val selected = empty.copy(
            item = SeasonWriteRequest("", "", null, id),
            included = mapOf("tournament" to listOf(mapOf("id" to id, "name" to "Winter Open"))),
        )
        assertEquals(empty.manifest, selected.manifest)
        val json = mapper.readTree(mapper.writeValueAsString(selected))
        assertEquals(id.toString(), json.path("item").path("tournamentId").stringValue())
        val included = json.path("included").path("tournament").path(0)
        assertEquals(id.toString(), included.path("id").stringValue())
        assertEquals("Winter Open", included.path("name").stringValue())
        assertEquals("", json.path("item").path("name").stringValue())
        assertEquals("", json.path("item").path("slug").stringValue())
        assertTrue(json.path("item").path("fromYear").isNull)
        val unselected = mapper.readTree(mapper.writeValueAsString(empty))
        assertTrue(unselected.path("item").path("tournamentId").isNull)
        assertEquals(0, unselected.path("included").size())
    }
}
