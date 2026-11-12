package com.abeesmall.tournup.manifest.tournament

import com.abeesmall.tournup.model.tournament.HandicapMode
import com.abeesmall.tournup.model.tournament.ParticipantType
import com.abeesmall.tournup.model.tournament.ParticipationMode
import com.abeesmall.tournup.support.UiTestSupport
import kotlin.test.Test
import kotlin.test.assertContains
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue
import org.junit.jupiter.api.extension.RegisterExtension

class TournamentFieldCatalogueTests {
    @RegisterExtension @JvmField val ui = UiTestSupport()

    @Test
    fun `UI choices and validation values cover the same enum entries`() {
        val fields = ui.manifests.fields(tournamentResourceDefinition())
        val expected = mapOf(
            "participantType" to ParticipantType.entries.map { it.name },
            "participationMode" to ParticipationMode.entries.map { it.name },
            "handicapMode" to HandicapMode.entries.map { it.name },
        )
        for ((id, values) in expected) {
            val field = fields.getValue(id)
            assertEquals(values, field.allowedValues)
            assertEquals(values, field.options.map { it.value })
            assertTrue(field.options.all { it.label.isNotBlank() })
        }
    }

    @Test
    fun `enum defaults are available choices`() {
        val fields = ui.manifests.fields(tournamentResourceDefinition())

        for (id in listOf("participantType", "participationMode", "handicapMode")) {
            val field = assertNotNull(fields[id])
            assertTrue(field.allowedValues.isNotEmpty())
            assertContains(
                field.allowedValues,
                assertNotNull(field.defaultValue),
            )
        }
    }
}
