package com.abeesmall.tournup.service

import com.abeesmall.tournup.dto.ui.LookupItem
import java.util.UUID
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull
import kotlin.test.assertNotNull
import org.junit.jupiter.api.Test
import org.springframework.data.domain.Sort
import org.springframework.web.server.ResponseStatusException

class ResourceQueriesTests {
    @Test
    fun `cursor continues after the last returned item and binds search and resource context`() {
        val rows = listOf("Alpha", "Alpha", "Beta").map { LookupItem(UUID.randomUUID(), it) }
        val first = LookupQuery.parse("tournament", " AL ", 2, null)
        val response = first.response(rows)
        assertEquals(rows.take(2), response.items)
        val cursor = assertNotNull(response.nextCursor)
        val next = LookupQuery.parse("tournament", "al", 2, cursor)
        assertEquals(rows[1].name, next.afterName)
        assertEquals(rows[1].id, next.afterId)
        assertEquals(3, next.pageable.pageSize)
        assertNull(next.response(emptyList()).nextCursor)
        assertFailsWith<ResponseStatusException> { LookupQuery.parse("season", "al", 2, cursor) }
        assertFailsWith<ResponseStatusException> { LookupQuery.parse("tournament", "other", 2, cursor) }
    }

    @Test
    fun `lookup treats user wildcards as literal characters`() {
        assertEquals("%a!_!%!!%", LookupQuery.parse("tournament", "a_%!", 20, null).pattern)
    }

    @Test
    fun `invalid query limits and cursors are rejected`() {
        for (limit in listOf(0, 101)) {
            assertFailsWith<ResponseStatusException> { LookupQuery.parse("tournament", "", limit, null) }
        }
        assertFailsWith<ResponseStatusException> { LookupQuery.parse("tournament", "x".repeat(256), 20, null) }
        for (cursor in listOf("", "invalid!", "a".repeat(4097))) {
            assertFailsWith<ResponseStatusException> { LookupQuery.parse("tournament", "", 20, cursor) }
        }
        assertFailsWith<ResponseStatusException> { resourcePage(-1, 20, Sort.by("id")) }
        assertFailsWith<ResponseStatusException> { resourcePage(0, 101, Sort.by("id")) }
        assertEquals(2, resourcePage(2, 10, Sort.by("id")).pageNumber)
    }
}
