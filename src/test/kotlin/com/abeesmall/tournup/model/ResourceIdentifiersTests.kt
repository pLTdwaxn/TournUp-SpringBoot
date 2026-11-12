package com.abeesmall.tournup.model

import kotlin.test.*
import org.junit.jupiter.api.Test

class ResourceIdentifiersTests {
    @Test
    fun `internal IDs have UUIDv7 version variant and timestamp`() {
        val before = System.currentTimeMillis()
        val id = ResourceIdentifiers.uuidV7()
        assertEquals(7, id.version())
        assertEquals(2, id.variant())
        assertTrue((id.mostSignificantBits ushr 16) in before..System.currentTimeMillis())
    }

    @Test
    fun `locators enforce the custom NanoID alphabet and length`() {
        for (symbol in "23456789BCDFGHJKLMNPQRSTVWXYZ") {
            val id = symbol.toString().repeat(4)
            assertEquals(id, ResourceIdentifiers.nanoId("season-$id"))
        }
        for (invalid in listOf("abcd", "B0DF", "B1DF", "BIDF", "BODF", "BADF", "B-DF", "BCD", "BCDFG")) {
            assertNull(ResourceIdentifiers.nanoId("season-$invalid"), invalid)
        }
    }

    @Test
    fun `public IDs resolve independently from the readable slug`() {
        val nanoId = ResourceIdentifiers.nanoId()
        assertTrue(nanoId.matches(Regex("[23456789BCDFGHJKLMNPQRSTVWXYZ]{4}")))
        assertEquals(nanoId, ResourceIdentifiers.nanoId("winter-open-$nanoId"))
        assertEquals(nanoId, ResourceIdentifiers.nanoId("renamed-$nanoId"))
        assertNull(ResourceIdentifiers.nanoId("winter-league"))
        assertNull(ResourceIdentifiers.nanoId(java.util.UUID.randomUUID().toString()))
    }
}
