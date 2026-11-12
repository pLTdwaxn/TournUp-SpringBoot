package com.abeesmall.tournup.validation

import com.abeesmall.tournup.resource.WriteContract
import com.abeesmall.tournup.resource.WriteOperation
import com.abeesmall.tournup.resource.season.seasonCreateContract
import com.abeesmall.tournup.resource.season.seasonUpdateContract
import com.abeesmall.tournup.resource.tournament.tournamentCreateContract
import com.abeesmall.tournup.resource.tournament.tournamentUpdateContract
import com.abeesmall.tournup.support.UiTestSupport
import com.abeesmall.tournup.ui.UiConstraintProjector
import com.abeesmall.tournup.ui.UiDataType
import com.abeesmall.tournup.ui.UiFieldPresentation
import jakarta.validation.Validation
import jakarta.validation.constraints.Pattern
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.RegisterExtension

class WriteContractMetadataTests {
    @RegisterExtension @JvmField val ui = UiTestSupport()

    data class ShortSlug(@field:ResourceSlug(max = 7) val slug: String)
    data class ServerPattern(@field:Pattern(regexp = "[A-Z]+") val code: String)

    @Test
    fun `provider configuration affects both validation and projected constraints`() {
        val xml = """
            <constraint-mappings xmlns="https://jakarta.ee/xml/ns/validation/mapping" version="3.0">
              <bean class="${ShortSlug::class.java.name}" ignore-annotations="false">
                <field name="slug" ignore-annotations="false">
                  <constraint annotation="jakarta.validation.constraints.Size">
                    <element name="max">3</element>
                  </constraint>
                </field>
              </bean>
            </constraint-mappings>
        """.trimIndent()
        Validation.byDefaultProvider().configure().addMapping(xml.byteInputStream())
            .buildValidatorFactory().use { factory ->
                val projector = UiConstraintProjector(WriteContractMetadata(factory.validator))
                val fields = projector.project(WriteContract(ShortSlug::class, WriteOperation.CREATE),
                    mapOf("slug" to UiFieldPresentation("Slug", UiDataType.STRING)))
                assertEquals(3, fields.getValue("slug").maxLength)
                assertTrue(factory.validator.validate(ShortSlug("four")).isNotEmpty())
            }
    }

    @Test
    fun `composed constraint overrides drive both server validation and manifest hints`() {
        val contract = WriteContract(ShortSlug::class, WriteOperation.CREATE)
        val field = ui.projector.project(contract,
            mapOf("slug" to UiFieldPresentation("Slug", UiDataType.STRING))).getValue("slug")
        assertEquals(7, field.maxLength)
        assertEquals(RESOURCE_SLUG_PATTERN, field.pattern)
        assertTrue(field.required)

        Validation.buildDefaultValidatorFactory().use { factory ->
            assertTrue(factory.validator.validate(ShortSlug("a-b")).isEmpty())
            assertTrue(factory.validator.validate(ShortSlug("too-long")).isNotEmpty())
            assertTrue(factory.validator.validate(ShortSlug("BAD")).isNotEmpty())
            assertTrue(factory.validator.validate(ShortSlug(" ")).isNotEmpty())
        }
    }

    @Test
    fun `value requiredness and JSON omission account for operation defaults`() {
        val create = ui.metadata.read(tournamentCreateContract.requestType).getValue("participantType")
        val update = ui.metadata.read(tournamentUpdateContract.requestType).getValue("participantType")
        assertTrue(create.required)
        assertTrue(create.acceptsOmission)
        assertTrue(create.hasDefault)
        assertEquals("PLAYER", tournamentCreateContract.defaultValue("participantType"))
        assertTrue(update.required)
        assertFalse(update.acceptsOmission)
        assertFalse(update.hasDefault)
        assertNull(tournamentUpdateContract.defaultValue("participantType"))
        assertFalse(ui.metadata.read(tournamentCreateContract.requestType).getValue("description").required)

        // Nullable transport values still fail @NotNull after binding an omitted field to null.
        val year = ui.metadata.read(seasonCreateContract.requestType).getValue("fromYear")
        assertTrue(year.acceptsOmission)
        assertTrue(year.required)
    }

    @Test
    fun `immutable field policy is scoped to the operation`() {
        assertTrue(seasonCreateContract.writable("tournamentId"))
        assertFalse(seasonUpdateContract.writable("tournamentId"))
        assertTrue(seasonUpdateContract.writable("name"))
        assertFalse(seasonUpdateContract.writable("lastAudit"))
        val request = requireNotNull(seasonCreateContract.initialValues).invoke()
        assertFailsWith<IllegalArgumentException> {
            seasonUpdateContract.requireUnchanged(request, emptyMap()) { IllegalStateException(it) }
        }
    }

    @Test
    fun `unsupported client pattern still validates on the server`() {
        val contract = WriteContract(ServerPattern::class, WriteOperation.CREATE)
        assertNull(ui.metadata.read(contract.requestType).getValue("code").pattern)
        Validation.buildDefaultValidatorFactory().use { factory ->
            assertTrue(factory.validator.validate(ServerPattern("lowercase")).isNotEmpty())
        }
    }
}
