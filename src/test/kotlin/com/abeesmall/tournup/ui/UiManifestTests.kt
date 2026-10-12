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
}
