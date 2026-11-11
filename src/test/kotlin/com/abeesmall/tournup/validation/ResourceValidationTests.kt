package com.abeesmall.tournup.validation

import jakarta.validation.Validation
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Test

class ResourceValidationTests {
    private val factory = Validation.buildDefaultValidatorFactory()
    private val validator = factory.validator

    private data class Example(
        @field:ResourceName val name: String?,
        @field:ResourceSlug val slug: String?,
    )

    private data class ShortName(@field:ResourceName(max = 5) val name: String)

    @AfterEach
    fun close() = factory.close()

    @Test
    fun `required field messages remain unchanged`() {
        val errors = validator.validate(Example(null, null)).associate { it.propertyPath.toString() to it.message }
        assertEquals(mapOf("name" to "Name is required", "slug" to "Slug is required"), errors)
    }

    @Test
    fun `length boundaries apply to both reusable constraints`() {
        assertTrue(validator.validate(Example("x".repeat(255), "x".repeat(255))).isEmpty())
        val errors = validator.validate(Example("x".repeat(256), "x".repeat(256)))
            .associate { it.propertyPath.toString() to it.message }
        assertEquals("Name must be at most 255 characters", errors["name"])
        assertEquals("Slug must be at most 255 characters", errors["slug"])
    }

    @Test
    fun `slug format is enforced and blank names are rejected`() {
        val errors = validator.validate(Example("   ", "Invalid_Slug"))
            .associate { it.propertyPath.toString() to it.message }
        assertEquals("Name is required", errors["name"])
        assertEquals("Use lowercase letters and digits separated by single hyphens", errors["slug"])
        assertTrue(validator.validate(Example("Winter Open", "winter-open-2026")).isEmpty())
    }

    @Test
    fun `a resource can override the shared maximum without duplicating constraints`() {
        assertTrue(validator.validate(ShortName("12345")).isEmpty())
        assertEquals("Name must be at most 5 characters", validator.validate(ShortName("123456")).single().message)
    }
}
