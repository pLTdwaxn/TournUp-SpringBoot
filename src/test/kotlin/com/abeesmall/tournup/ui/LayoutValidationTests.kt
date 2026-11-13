package com.abeesmall.tournup.ui

import com.abeesmall.tournup.manifest.tournamentResourceDefinition
import com.abeesmall.tournup.support.UiTestSupport
import kotlin.test.Test
import kotlin.test.assertEquals
import org.junit.jupiter.api.extension.RegisterExtension

class LayoutValidationTests {
    @RegisterExtension
    @JvmField
    val ui = UiTestSupport()

    @Test
    fun `reference form fields require a record select widget`() {
        val definition = LayoutDefinition(
            title = "Create season",
            layout = UiLayout(
                type = UiLayoutType.FORM,
                sections = listOf(UiSection("details", "Details", listOf("tournamentId"))),
            ),
            fields = mapOf("tournamentId" to FieldPresentation("Tournament", UiWidget.RECORD_SELECT)),
        )
        val types = mapOf("tournamentId" to UiDataType.REFERENCE)

        assertEquals(true, validateLayoutDefinition(definition, types, setOf("tournamentId")).isValid)
        assertEquals(
            setOf("tournamentId"),
            incompatibleWidgetFieldIds(
                definition.copy(fields = mapOf("tournamentId" to FieldPresentation("Tournament", UiWidget.TEXT))),
                types,
            ),
        )
        assertEquals(
            setOf("tournamentId"),
            incompatibleWidgetFieldIds(definition, mapOf("tournamentId" to UiDataType.STRING)),
        )
    }

    @Test
    fun `reports fields absent from the catalogue`() {
        val definition = LayoutDefinition(
            title = "Create tournament",
            layout = UiLayout(
                type = UiLayoutType.FORM,
                sections = listOf(
                    UiSection(
                        id = "details",
                        title = "Details",
                        fields = listOf("name", "unknownField"),
                    ),
                ),
            ),
            fields = mapOf(
                "name" to FieldPresentation("Name", UiWidget.TEXT),
                "madeUpField" to FieldPresentation("Made up", UiWidget.TEXT),
            ),
        )

        assertEquals(
            setOf("unknownField", "madeUpField"),
            unknownLayoutFieldIds(
                definition,
                ui.manifests.fields(tournamentResourceDefinition()).keys,
            ),
        )
    }

    @Test
    fun `reports incompatible widgets`() {
        val definition = LayoutDefinition(
            title = "Create tournament",
            layout = UiLayout(
                type = UiLayoutType.FORM,
                sections = listOf(
                    UiSection(
                        id = "details",
                        title = "Details",
                        fields = listOf("name", "participantType", "description"),
                    ),
                ),
            ),
            fields = mapOf(
                "name" to FieldPresentation("Name", UiWidget.SELECT),
                "participantType" to FieldPresentation("Participant type", UiWidget.TEXT),
                "description" to FieldPresentation("Description", UiWidget.TEXTAREA),
            ),
        )

        assertEquals(
            setOf("name", "participantType"),
            incompatibleWidgetFieldIds(
                definition,
                ui.manifests.fields(tournamentResourceDefinition())
                    .mapValues { it.value.dataType }
            ),
        )
    }

    @Test
    fun `reports required fields without defaults missing from form`() {
        val definition = LayoutDefinition(
            title = "Create tournament",
            layout = UiLayout(
                type = UiLayoutType.FORM,
                sections = listOf(
                    UiSection(
                        id = "details",
                        title = "Details",
                        fields = listOf("name"),
                    ),
                ),
            ),
            fields = mapOf(
                "name" to FieldPresentation("Name", UiWidget.TEXT),
            ),
        )

        val requiredWithoutDefaults = ui.manifests.fields(tournamentResourceDefinition())
            .filterValues { it.required && it.defaultValue == null }
            .keys

        assertEquals(
            setOf("slug"),
            omittedRequiredFieldIds(definition, requiredWithoutDefaults),
        )
    }

    @Test
    fun `reports placed fields without presentation`() {
        val definition = LayoutDefinition(
            title = "Create tournament",
            layout = UiLayout(
                type = UiLayoutType.FORM,
                sections = listOf(
                    UiSection(
                        id = "details",
                        title = "Details",
                        fields = listOf("name", "slug"),
                    ),
                ),
            ),
            fields = mapOf(
                "name" to FieldPresentation("Name", UiWidget.TEXT),
            ),
        )

        assertEquals(
            setOf("slug"),
            missingPresentationFieldIds(definition),
        )
    }

    @Test
    fun `reports fields placed more than once`() {
        val definition = LayoutDefinition(
            title = "Create tournament",
            layout = UiLayout(
                type = UiLayoutType.FORM,
                sections = listOf(
                    UiSection("details", "Details", listOf("name", "slug")),
                    UiSection("more", "More", listOf("name")),
                ),
            ),
            fields = mapOf(
                "name" to FieldPresentation("Name", UiWidget.TEXT),
                "slug" to FieldPresentation("Slug", UiWidget.TEXT),
            ),
        )

        assertEquals(setOf("name"), duplicatePlacedFieldIds(definition))
    }

    @Test
    fun `combines layout validation errors`() {
        val definition = LayoutDefinition(
            title = "Create tournament",
            layout = UiLayout(
                UiLayoutType.FORM,
                listOf(
                    UiSection(
                        "details",
                        "Details",
                        listOf("name", "name", "participantType", "ghost"),
                    ),
                ),
            ),
            fields = mapOf(
                "name" to FieldPresentation("Name", UiWidget.TEXT),
                "participantType" to FieldPresentation("Participant type", UiWidget.TEXT),
                "orphan" to FieldPresentation("Orphan", UiWidget.TEXT),
            ),
        )
        val catalogue = ui.manifests.fields(tournamentResourceDefinition())

        val report = validateLayoutDefinition(
            definition,
            catalogue.mapValues { it.value.dataType },
            catalogue.filterValues { it.required && it.defaultValue == null }.keys,
        )

        assertEquals(
            LayoutValidationReport(
                unknownFields = setOf("ghost", "orphan"),
                incompatibleWidgets = setOf("participantType"),
                omittedRequiredFields = setOf("slug"),
                missingPresentations = setOf("ghost"),
                duplicatePlacements = setOf("name"),
            ),
            report,
        )
        assertEquals(false, report.isValid)
    }

    @Test
    fun `reports table errors without requiring creation fields`() {
        val definition = LayoutDefinition(
            title = "Tournaments",
            layout = UiLayout(
                type = UiLayoutType.TABLE,
                columns = listOf(
                    "name", "name", "participantType", "ghost",
                ),
            ),
            fields = mapOf(
                "name" to FieldPresentation("Name", UiWidget.DISPLAY),
                "participantType" to FieldPresentation(
                    "Participant type", UiWidget.SELECT,
                ),
                "orphan" to FieldPresentation("Orphan", UiWidget.DISPLAY),
            ),
        )
        val catalogue = ui.manifests.fields(tournamentResourceDefinition())

        val report = validateLayoutDefinition(
            definition,
            catalogue.mapValues { it.value.dataType },
            catalogue.filterValues {
                it.required && it.defaultValue == null
            }.keys,
        )

        assertEquals(
            LayoutValidationReport(
                unknownFields = setOf("ghost", "orphan"),
                incompatibleWidgets = setOf("participantType"),
                omittedRequiredFields = emptySet(),
                missingPresentations = setOf("ghost"),
                duplicatePlacements = setOf("name"),
            ),
            report,
        )
        assertEquals(false, report.isValid)
    }

    @Test
    fun `accepts display columns without requiring creation fields`() {
        val definition = LayoutDefinition(
            title = "Tournaments",
            layout = UiLayout(
                type = UiLayoutType.TABLE,
                columns = listOf("participantType"),
            ),
            fields = mapOf(
                "participantType" to FieldPresentation(
                    "Participant type", UiWidget.DISPLAY,
                ),
            ),
        )

        val report = validateLayoutDefinition(
            definition,
            mapOf("participantType" to UiDataType.ENUM),
            requiredWithoutDefaults = setOf("name", "slug"),
        )

        assertEquals(true, report.isValid)
    }

    @Test
    fun `accepts a number widget for a required integer form field`() {
        val definition = LayoutDefinition(
            title = "Create season",
            layout = UiLayout(
                type = UiLayoutType.FORM,
                sections = listOf(
                    UiSection("details", "Details", listOf("fromYear")),
                ),
            ),
            fields = mapOf(
                "fromYear" to FieldPresentation("From year", UiWidget.NUMBER),
            ),
        )

        val report = validateLayoutDefinition(
            definition,
            mapOf("fromYear" to UiDataType.INTEGER),
            requiredWithoutDefaults = setOf("fromYear"),
        )

        assertEquals(true, report.isValid)
    }

    @Test
    fun `rejects mismatched integer and string form widgets`() {
        val definition = LayoutDefinition(
            title = "Create season",
            layout = UiLayout(
                type = UiLayoutType.FORM,
                sections = listOf(
                    UiSection("details", "Details", listOf("fromYear", "name")),
                ),
            ),
            fields = mapOf(
                "fromYear" to FieldPresentation("From year", UiWidget.TEXT),
                "name" to FieldPresentation("Name", UiWidget.NUMBER),
            ),
        )

        assertEquals(
            setOf("fromYear", "name"),
            incompatibleWidgetFieldIds(
                definition,
                mapOf(
                    "fromYear" to UiDataType.INTEGER,
                    "name" to UiDataType.STRING,
                ),
            ),
        )
    }
}
