package com.abeesmall.tournup.manifest.tournament

import kotlin.test.Test
import kotlin.test.assertContains
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class TournamentFieldCatalogueTests {
    @Test
    fun `enum defaults are available choices`() {
        val fields = tournamentCreateFieldCatalogue()

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
