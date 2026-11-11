package com.abeesmall.tournup.ui.resource

import com.abeesmall.tournup.dto.ui.UiResourceIdentifier
import com.abeesmall.tournup.dto.ui.UiResourceRelationship
import com.abeesmall.tournup.ui.UiApplication
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import org.junit.jupiter.api.Test

class UiResourceRegistryTests {
    @UiResource("sample")
    private class First

    @UiResource("sample")
    private class Duplicate

    @UiResource("")
    private class EmptyKey

    private class Unmarked

    @Test
    fun `registers only explicitly supplied annotated models`() {
        val registry = UiResourceRegistry(listOf(registration(First::class.java)))

        assertEquals(setOf("sample"), registry.resources.keys)
        assertEquals(First::class.java, registry.resources.getValue("sample").modelClass)
    }

    @Test
    fun `duplicate resource keys are rejected`() {
        assertFailsWith<IllegalArgumentException> {
            UiResourceRegistry(listOf(registration(First::class.java), registration(Duplicate::class.java)))
        }
    }

    @Test
    fun `unmarked models cannot be registered`() {
        assertFailsWith<IllegalArgumentException> {
            UiResourceRegistry(listOf(registration(Unmarked::class.java)))
        }
    }

    @Test
    fun `empty resource keys are rejected`() {
        assertFailsWith<IllegalArgumentException> {
            UiResourceRegistry(listOf(registration(EmptyKey::class.java)))
        }
    }

    @Test
    fun `relationships cannot target an unregistered resource`() {
        assertFailsWith<IllegalArgumentException> {
            UiResourceRegistry(
                listOf(
                    registration(First::class.java).copy(
                        relationships = listOf(UiResourceRelationship("parentId", "missing", "Parent", true)),
                    ),
                ),
            )
        }
    }

    private fun registration(model: Class<*>): UiResourceRegistration = UiResourceRegistration(
        modelClass = model,
        applications = setOf(UiApplication.TOURNAMENT_MANAGER),
        label = "Samples",
        displayField = null,
        fields = emptyMap(),
        identifier = UiResourceIdentifier("id", "uuid"),
        relationships = emptyList(),
        operations = emptyList(),
        canRead = { it.isAuthenticated },
        canWrite = { false },
    )
}
