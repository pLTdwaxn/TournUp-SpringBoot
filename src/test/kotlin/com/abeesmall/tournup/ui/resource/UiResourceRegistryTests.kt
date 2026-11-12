package com.abeesmall.tournup.ui.resource

import com.abeesmall.tournup.dto.ui.UiResourceIdentifier
import com.abeesmall.tournup.dto.ui.UiResourceRelationship
import com.abeesmall.tournup.resource.Resource
import com.abeesmall.tournup.resource.ResourceDefinition
import com.abeesmall.tournup.support.registryFor
import com.abeesmall.tournup.ui.UiApplication
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import org.junit.jupiter.api.Test

class UiResourceRegistryTests {
    @Resource(key = "sample", pluralName = "samples")
    private class First

    @Resource(key = "sample", pluralName = "other-samples")
    private class Duplicate

    @Resource(key = "", pluralName = "samples")
    private class EmptyKey

    @Resource(key = "other", pluralName = "samples")
    private class DuplicatePlural

    @Resource(key = "sample", pluralName = "")
    private class EmptyPlural

    @Resource(key = "sample", pluralName = "Samples")
    private class UppercasePlural

    @Resource(key = "sample", pluralName = "../samples")
    private class PathPlural

    @Resource(key = "person", pluralName = "people")
    private class Person

    private class Unmarked

    @Test
    fun `registers only explicitly supplied annotated DTOs`() {
        val registry = uiRegistry(listOf(registration(First::class.java)))

        assertEquals(setOf("sample"), registry.resources.keys)
        assertEquals(First::class.java, registry.resources.getValue("sample").definition.resourceClass)
        assertEquals("samples", registry.resources.getValue("sample").definition.pluralName)
    }

    @Test
    fun `plural names come from DTO metadata including irregular plurals`() {
        val registry = uiRegistry(listOf(registration(Person::class.java)))

        assertEquals("people", registry.resources.getValue("person").definition.pluralName)
    }

    @Test
    fun `plural names must be unique across resources`() {
        assertFailsWith<IllegalArgumentException> {
            uiRegistry(listOf(registration(First::class.java), registration(DuplicatePlural::class.java)))
        }
    }

    @Test
    fun `plural names must be nonempty lowercase URL segments`() {
        for (model in listOf(EmptyPlural::class.java, UppercasePlural::class.java, PathPlural::class.java)) {
            assertFailsWith<IllegalArgumentException> {
                uiRegistry(listOf(registration(model)))
            }
        }
    }

    @Test
    fun `duplicate resource keys are rejected`() {
        assertFailsWith<IllegalArgumentException> {
            uiRegistry(listOf(registration(First::class.java), registration(Duplicate::class.java)))
        }
    }

    @Test
    fun `unmarked DTOs cannot be registered`() {
        assertFailsWith<IllegalArgumentException> {
            uiRegistry(listOf(registration(Unmarked::class.java)))
        }
    }

    @Test
    fun `empty resource keys are rejected`() {
        assertFailsWith<IllegalArgumentException> {
            uiRegistry(listOf(registration(EmptyKey::class.java)))
        }
    }

    @Test
    fun `relationships cannot target an unregistered resource`() {
        assertFailsWith<IllegalArgumentException> {
            uiRegistry(
                listOf(
                    registration(First::class.java).copy(
                        relationships = listOf(UiResourceRelationship("parentId", "missing", "Parent", true)),
                    ),
                ),
            )
        }
    }

    private fun uiRegistry(registrations: List<UiResourceRegistration>) =
        UiResourceRegistry(registrations, registryFor(registrations.map { it.definition }))

    private fun registration(model: Class<*>): UiResourceRegistration = UiResourceRegistration(
        definition = ResourceDefinition(model, canRead = { it.isAuthenticated }, canWrite = { false }),
        audited = false,
        applications = setOf(UiApplication.TOURNAMENT_MANAGER),
        label = "Samples",
        displayField = null,
        presentation = emptyMap(),
        identifier = UiResourceIdentifier("id", "uuid"),
        relationships = emptyList(),
        operations = emptyList(),
    )
}
