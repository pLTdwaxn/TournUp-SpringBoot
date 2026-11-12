package com.abeesmall.tournup.controller.tournament

import com.abeesmall.tournup.model.season.Season
import com.abeesmall.tournup.model.tournament.Tournament
import com.abeesmall.tournup.dto.audit.LastAuditData
import com.abeesmall.tournup.repository.audit.LatestAuditReader
import com.abeesmall.tournup.repository.tournament.TournamentRepository
import com.abeesmall.tournup.service.tournament.TournamentService
import com.abeesmall.tournup.security.SecurityConfiguration
import com.abeesmall.tournup.support.ResourceWebTestConfiguration
import com.abeesmall.tournup.ui.resource.UiResourceRegistry
import com.abeesmall.tournup.manifest.season.SeasonManifestConfiguration
import com.abeesmall.tournup.manifest.tournament.TournamentManifestConfiguration
import java.time.Instant
import java.util.UUID
import org.hamcrest.Matchers.equalTo
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import org.mockito.Mockito.`when` as whenever
import org.mockito.Mockito.verify
import org.mockito.Mockito.verifyNoInteractions
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest
import org.springframework.context.annotation.Import
import org.springframework.test.context.bean.override.mockito.MockitoBean
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.get

@WebMvcTest(TournamentController::class)
@Import(SecurityConfiguration::class, ResourceWebTestConfiguration::class, TournamentService::class,
    UiResourceRegistry::class, SeasonManifestConfiguration::class, TournamentManifestConfiguration::class)
class TournamentDetailControllerTests(@Autowired private val mvc: MockMvc) {
    @MockitoBean private lateinit var records: TournamentRepository
    @MockitoBean private lateinit var audits: LatestAuditReader

    private val record = Tournament(slug = "winter-open", name = "Winter Open")

    @Test
    fun `authentication is required`() {
        mvc.get("/api/tournaments/${record.locator}/").andExpect { status { isUnauthorized() } }
        verifyNoInteractions(records, audits)
    }

    @ParameterizedTest
    @ValueSource(strings = ["SUPER_ADMIN", "CLUB_MANAGER", "TOURNAMENT_MANAGER", "PLAYER", "COACH"])
    fun `public detail uses its locator and never serializes internal keys`(role: String) {
        whenever(records.findByNanoId(record.nanoId)).thenReturn(record)
        mvc.get("/api/tournaments/${record.locator}/") { with(user("viewer").roles(role)) }.andExpect {
            status { isOk() }
            jsonPath("$.item.id", equalTo(record.locator))
            jsonPath("$.item.nanoId", equalTo(record.nanoId))
            jsonPath("$.item.internalId") { doesNotHaveJsonPath() }
            jsonPath("$.item.name", equalTo(record.name))
        }
        verify(records).findByNanoId(record.nanoId)
    }

    @Test
    fun `UUIDs and bare slugs are no longer public identifiers`() {
        for (identifier in listOf(record.id.toString(), "winter-championship")) {
            mvc.get("/api/tournaments/$identifier/") { with(user("reader").roles("PLAYER")) }
                .andExpect { status { isNotFound() } }
        }
        verifyNoInteractions(records, audits)
    }

    @Test
    fun `unknown NanoID returns not found without an audit lookup`() {
        mvc.get("/api/tournaments/missing-ZZZZ/") { with(user("reader").roles("PLAYER")) }
            .andExpect { status { isNotFound() } }
        verify(records).findByNanoId("ZZZZ")
        verifyNoInteractions(audits)
    }

    @Test
    fun `renaming a slug keeps old locators resolvable and returns the canonical locator`() {
        val old = record.locator
        record.slug = "renamed"
        whenever(records.findByNanoId(record.nanoId)).thenReturn(record)
        mvc.get("/api/tournaments/$old/") { with(user("reader").roles("PLAYER")) }.andExpect {
            status { isOk() }
            jsonPath("$.item.id", equalTo(record.locator))
        }
    }

    @Test
    fun `audit enrichment uses the hidden UUID rather than the public locator`() {
        whenever(records.findByNanoId(record.nanoId)).thenReturn(record)
        whenever(audits.findLatest(Tournament::class.java, listOf(record.id))).thenReturn(
            mapOf(record.id to LastAuditData(42, Instant.parse("2026-11-12T00:00:00Z"), UUID.randomUUID())))
        mvc.get("/api/tournaments/${record.locator}/") { with(user("admin").roles("SUPER_ADMIN")) }.andExpect {
            status { isOk() }
            jsonPath("$.item.lastAudit.revision", equalTo(42))
            jsonPath("$.item.internalId") { doesNotHaveJsonPath() }
        }
        verify(audits).findLatest(Tournament::class.java, listOf(record.id))
    }
}
