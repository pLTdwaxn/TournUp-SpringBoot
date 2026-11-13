package com.abeesmall.tournup.ui

import com.abeesmall.tournup.dto.ui.UiCollectionResponse
import com.abeesmall.tournup.dto.ui.UiPagination
import com.abeesmall.tournup.dto.ui.UiRecordResponse
import com.abeesmall.tournup.manifest.seasonResourceDefinition
import com.abeesmall.tournup.manifest.tournament.tournamentResourceDefinition
import com.abeesmall.tournup.support.UiTestSupport
import java.nio.file.Files
import java.nio.file.Path
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.RegisterExtension
import tools.jackson.databind.json.JsonMapper

class UiResourceResponseTests {
    @RegisterExtension
    @JvmField
    val ui = UiTestSupport()

    private val mapper = JsonMapper.builder().build()

    @Test
    fun `sample detail manifest matches the concise wire contract`() {
        val manifest = ui.manifests.resolve(
            seasonResourceDefinition(),
            UiManifestContext(
                UiApplication.TOURNAMENT_MANAGER, UiOperation.READ,
                allowedWrites = setOf(UiOperation.UPDATE), title = "2026 S1",
                recordId = "2026-s1-GHJK",
            ),
        )
        val json = mapper.readTree(mapper.writeValueAsString(manifest))
        val sample = mapper.readTree(Files.readString(Path.of("examples/api/season-detail.response.json")))
        assertEquals(sample.path("manifest"), json)
        for (field in json.path("fields")) {
            assertFalse(field.has("required"))
            assertFalse(field.has("options"))
            assertFalse(field.has("behaviours"))
            assertFalse(field.has("maxLength"))
            assertFalse(field.has("pattern"))
            assertTrue(field.has("editable"))
            assertFalse(field.path("editable").booleanValue())
        }
    }

    @Test
    fun `form serialization preserves explicit optional and readonly flags`() {
        val manifest = ui.manifests.resolve(
            tournamentResourceDefinition(),
            UiManifestContext(UiApplication.TOURNAMENT_MANAGER, UiOperation.CREATE),
        )
        val json = mapper.readTree(mapper.writeValueAsString(manifest))
        val description = json.path("fields").path("description")
        assertTrue(description.has("required"))
        assertFalse(description.path("required").booleanValue())
        assertFalse(description.path("editable").booleanValue())
        assertFalse(json.path("layout").has("columns"))
    }

    @Test
    fun `record and collection envelopes retain empty data and inclusion containers`() {
        val manifest = ui.manifests.resolve(
            seasonResourceDefinition(), UiManifestContext(UiApplication.TOURNAMENT_MANAGER, UiOperation.LIST),
        )
        val record = mapper.readTree(mapper.writeValueAsString(UiRecordResponse(manifest, mapOf("name" to ""))))
        assertFalse(record.has("context"))
        assertTrue(record.path("included").isObject)
        val collection = mapper.readTree(
            mapper.writeValueAsString(
                UiCollectionResponse(manifest, emptyList<String>(), UiPagination(0, 20, 0, 0)),
            )
        )
        assertTrue(collection.path("items").isArray)
        assertEquals(0, collection.path("items").size())
        assertTrue(collection.path("included").isObject)
        assertEquals(0L, collection.path("pagination").path("totalElements").longValue())
    }
}
