package com.abeesmall.tournup.manifest.season

import com.abeesmall.tournup.dto.season.SEASON_NAME_MAX_LENGTH
import com.abeesmall.tournup.dto.season.SEASON_SLUG_MAX_LENGTH
import com.abeesmall.tournup.dto.season.SEASON_SLUG_PATTERN
import com.abeesmall.tournup.dto.season.SeasonCreateContext
import com.abeesmall.tournup.dto.season.SeasonCreateFormResponse
import java.util.UUID
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import org.junit.jupiter.api.Test
import tools.jackson.databind.json.JsonMapper

class SeasonCreateManifestTests {
    private val mapper = JsonMapper.builder().build()

    @Test
    fun `form wire contract defines only editable season inputs in dependency order`() {
        val tournamentId = UUID.randomUUID()
        val json = mapper.readTree(
            mapper.writeValueAsString(seasonCreateManifest(tournamentId)),
        )

        assertEquals(1, json.path("contractVersion").intValue())
        assertEquals("tournament-manager", json.path("application").stringValue())
        assertEquals("season", json.path("resource").stringValue())
        assertEquals("create", json.path("operation").stringValue())
        assertEquals("form", json.path("layout").path("type").stringValue())
        assertEquals(
            listOf("fromYear", "name", "slug"),
            json.path("layout").path("sections").path(0).path("fields")
                .valueStream().map { it.stringValue() }.toList(),
        )

        val fields = json.path("fields")
        assertEquals(3, fields.size())
        for (id in listOf("fromYear", "name", "slug")) {
            assertTrue(fields.path(id).path("required").booleanValue())
            assertTrue(fields.path(id).path("editable").booleanValue())
        }
        assertEquals("integer", fields.path("fromYear").path("dataType").stringValue())
        assertEquals("number", fields.path("fromYear").path("widget").stringValue())
        assertEquals("text", fields.path("name").path("widget").stringValue())
        assertEquals("text", fields.path("slug").path("widget").stringValue())
        assertEquals(
            SEASON_NAME_MAX_LENGTH,
            fields.path("name").path("maxLength").intValue(),
        )
        assertEquals(
            SEASON_SLUG_MAX_LENGTH,
            fields.path("slug").path("maxLength").intValue(),
        )
        assertEquals(
            SEASON_SLUG_PATTERN,
            fields.path("slug").path("pattern").stringValue(),
        )

        val command = json.path("commands").path(0)
        assertEquals("POST", command.path("method").stringValue())
        assertEquals(
            "/api/tournaments/$tournamentId/seasons/",
            command.path("href").stringValue(),
        )
    }

    @Test
    fun `name and slug behaviours use their agreed sources and wire names`() {
        val json = mapper.readTree(
            mapper.writeValueAsString(seasonCreateManifest(UUID.randomUUID())),
        )
        val fields = json.path("fields")
        val nameBehaviour = fields.path("name").path("behaviours").path(0)
        val slugBehaviour = fields.path("slug").path("behaviours").path(0)

        assertEquals(
            "season-name-from-year",
            nameBehaviour.path("type").stringValue(),
        )
        assertEquals("fromYear", nameBehaviour.path("sourceField").stringValue())
        assertEquals("slugify", slugBehaviour.path("type").stringValue())
        assertEquals("name", slugBehaviour.path("sourceField").stringValue())
    }

    @Test
    fun `form supplies naming context without calculating a name on the server`() {
        val tournamentId = UUID.randomUUID()
        val response = SeasonCreateFormResponse(
            manifest = seasonCreateManifest(tournamentId),
            context = SeasonCreateContext(
                tournamentId = tournamentId,
                existingNamesByYear = mapOf(
                    2026 to listOf("2026 S1", "2026 S3", "Winter league"),
                    2027 to listOf("2027 S2"),
                ),
            ),
        )
        val json = mapper.readTree(mapper.writeValueAsString(response))

        assertEquals(
            tournamentId.toString(),
            json.path("context").path("tournamentId").stringValue(),
        )
        val names = json.path("context").path("existingNamesByYear")
        assertEquals(
            listOf("2026 S1", "2026 S3", "Winter league"),
            names.path("2026").valueStream().map { it.stringValue() }.toList(),
        )
        assertEquals(
            listOf("2027 S2"),
            names.path("2027").valueStream().map { it.stringValue() }.toList(),
        )
        assertEquals("", json.path("item").path("name").stringValue())
        assertEquals("", json.path("item").path("slug").stringValue())
        assertTrue(json.path("item").path("fromYear").isNull)
    }
}
