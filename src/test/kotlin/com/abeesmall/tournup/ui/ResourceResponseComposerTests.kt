package com.abeesmall.tournup.ui

import com.abeesmall.tournup.dto.audit.LastAuditData
import com.abeesmall.tournup.dto.season.SeasonData
import com.abeesmall.tournup.dto.ui.UiCollectionResponse
import com.abeesmall.tournup.dto.ui.UiPagination
import com.abeesmall.tournup.dto.ui.UiRecordResponse
import com.abeesmall.tournup.manifest.season.seasonResourceDefinition
import com.abeesmall.tournup.model.season.Season
import com.abeesmall.tournup.repository.audit.LatestAuditReader
import com.abeesmall.tournup.support.UiTestSupport
import com.abeesmall.tournup.ui.UiApplication
import com.abeesmall.tournup.ui.UiManifestContext
import com.abeesmall.tournup.ui.UiOperation
import com.abeesmall.tournup.ui.withLatestAudit
import java.time.Instant
import java.util.UUID
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.RegisterExtension
import org.mockito.Mockito.`when` as whenever
import org.mockito.Mockito.mock
import org.mockito.Mockito.verify
import org.mockito.Mockito.verifyNoInteractions
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.authority.SimpleGrantedAuthority

class ResourceResponseComposerTests {
    @RegisterExtension @JvmField val ui = UiTestSupport()

    private val reader = mock(LatestAuditReader::class.java)
    private val item = SeasonData(UUID.randomUUID(), UUID.randomUUID(), "2026 S1", "2026-s1", 2026)
    private val manifest = ui.manifests.resolve(seasonResourceDefinition(),
        UiManifestContext(UiApplication.TOURNAMENT_MANAGER, UiOperation.READ, title = item.name))
    private fun viewer(role: String) = UsernamePasswordAuthenticationToken("user", null,
        listOf(SimpleGrantedAuthority("ROLE_$role")))

    @Test
    fun `record enrichment preserves data and includes permitted audit metadata`() {
        whenever(reader.findLatest(Season::class.java, listOf(item.id))).thenReturn(
            mapOf(item.id to LastAuditData(7, Instant.parse("2026-11-12T00:00:00Z"), null)),
        )
        val result = UiRecordResponse(manifest, item).withLatestAudit(Season::class.java, viewer("SUPER_ADMIN"), reader)
        assertEquals(item.id, result.item.id)
        assertNotNull(result.item.lastAudit)
        verify(reader).findLatest(Season::class.java, listOf(item.id))
    }

    @Test
    fun `denied readers and empty collections perform no audit queries`() {
        val result = UiRecordResponse(manifest, item).withLatestAudit(Season::class.java, viewer("PLAYER"), reader)
        assertNull(result.item.lastAudit)
        val empty = UiCollectionResponse<SeasonData>(manifest, emptyList(), UiPagination(0, 20, 0, 0))
        assertEquals(empty, empty.withLatestAudit(Season::class.java, viewer("SUPER_ADMIN"), reader))
        verifyNoInteractions(reader)
    }
}
