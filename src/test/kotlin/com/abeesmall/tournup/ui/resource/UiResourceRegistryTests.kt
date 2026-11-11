package com.abeesmall.tournup.ui.resource

import com.abeesmall.tournup.dto.ui.UiResourceIdentifier
import com.abeesmall.tournup.dto.ui.UiResourceRelationship
import com.abeesmall.tournup.ui.UiApplication
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import org.junit.jupiter.api.Test

class UiResourceRegistryTests {
    @UiResource(key = "sample", pluralName = "samples")
    private class First

    @UiResource(key = "sample", pluralName = "other-samples")
    private class Duplicate

    @UiResource(key = "", pluralName = "samples")
    private class EmptyKey

    @UiResource(key = "other", pluralName = "samples")
    private class DuplicatePlural

    @UiResource(key = "sample", pluralName = "")
    private class EmptyPlural

    @UiResource(key = "sample", pluralName = "Samples")
    private class UppercasePlural

    @UiResource(key = "sample", pluralName = "../samples")
    private class PathPlural

    @UiResource(key = "person", pluralName = "people")
    private class Person

    private class Unmarked

    @Test
    fun `registers only explicitly supplied annotated models`() {
        val registry = UiResourceRegistry(listOf(registration(First::class.java)))

        assertEquals(setOf("sample"), registry.resources.keys)
        assertEquals(First::class.java, registry.resources.getValue("sample").modelClass)
        assertEquals("samples", registry.resources.getValue("sample").pluralName)
    }

    @Test
    fun `plural names come from model metadata including irregular plurals`() {
        val registry = UiResourceRegistry(listOf(registration(Person::class.java)))

        assertEquals("people", registry.resources.getValue("person").pluralName)
    }

    @Test
    fun `plural names must be unique across resources`() {
        assertFailsWith<IllegalArgumentException> {
            UiResourceRegistry(listOf(registration(First::class.java), registration(DuplicatePlural::class.java)))
        }
    }

    @Test
    fun `plural names must be nonempty lowercase URL segments`() {
        for (model in listOf(EmptyPlural::class.java, UppercasePlural::class.java, PathPlural::class.java)) {
            assertFailsWith<IllegalArgumentException> {
                UiResourceRegistry(listOf(registration(model)))
            }
        }
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
