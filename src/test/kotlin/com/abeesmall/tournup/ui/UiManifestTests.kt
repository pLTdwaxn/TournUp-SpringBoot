package com.abeesmall.tournup.ui

import kotlin.test.Test
import kotlin.test.assertEquals
import tools.jackson.databind.json.JsonMapper

class UiManifestTests {
    private val mapper = JsonMapper.builder().build()

    @Test
    fun `contract enums use their wire values`() {
        assertEquals(
            "\"create\"",
            mapper.writeValueAsString(UiOperation.CREATE),
        )
        assertEquals(
            "\"form\"",
            mapper.writeValueAsString(UiLayoutType.FORM),
        )
    }

    @Test
    fun `table contract preserves wire values and column order`() {
        val layout = UiLayout(
            type = UiLayoutType.TABLE,
            columns = listOf("participantType", "name"),
        )
        val json = mapper.readTree(mapper.writeValueAsString(layout))

        assertEquals("table", json.path("type").stringValue())
        assertEquals(
            listOf("participantType", "name"),
            json.path("columns")
                .valueStream()
                .map { it.stringValue() }
                .toList(),
        )
        assertEquals("\"list\"", mapper.writeValueAsString(UiOperation.LIST))
        assertEquals("\"display\"", mapper.writeValueAsString(UiWidget.DISPLAY))
    }
}
