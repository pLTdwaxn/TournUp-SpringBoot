package com.abeesmall.tournup.controller.ui

import com.abeesmall.tournup.controller.season.SeasonController
import com.abeesmall.tournup.controller.tournament.TournamentController
import com.abeesmall.tournup.dto.ui.LookupItem
import com.abeesmall.tournup.dto.ui.LookupResponse
import com.abeesmall.tournup.manifest.SeasonManifestConfiguration
import com.abeesmall.tournup.manifest.TournamentManifestConfiguration
import com.abeesmall.tournup.repository.audit.LatestAuditReader
import com.abeesmall.tournup.security.SecurityConfiguration
import com.abeesmall.tournup.service.season.SeasonService
import com.abeesmall.tournup.service.tournament.TournamentService
import com.abeesmall.tournup.support.ResourceWebTestConfiguration
import org.hamcrest.Matchers.equalTo
import org.junit.jupiter.api.Test
import org.mockito.Mockito.`when` as whenever
import org.mockito.Mockito.verifyNoInteractions
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest
import org.springframework.context.annotation.Import
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user
import org.springframework.test.context.bean.override.mockito.MockitoBean
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.get

@WebMvcTest(SeasonController::class, TournamentController::class)
@Import(
    SecurityConfiguration::class,
    com.abeesmall.tournup.ui.resource.UiResourceRegistry::class,
    SeasonManifestConfiguration::class,
    TournamentManifestConfiguration::class,
    ResourceWebTestConfiguration::class,
)
class ResourceLookupControllerTests(@Autowired private val mvc: MockMvc) {
    @MockitoBean
    private lateinit var seasons: SeasonService

    @MockitoBean
    private lateinit var tournaments: TournamentService

    @MockitoBean
    private lateinit var audits: LatestAuditReader

    @Test
    fun `lookup returns lightweight batches without page manifests or audits`() {
        val id = "item-BCDF"
        val parent = "parent-BCDF"
        whenever(tournaments.lookup("winter", 20, null)).thenReturn(
            LookupResponse(
                listOf(
                    LookupItem(
                        id,
                        "Winter Open"
                    )
                ), null
            )
        )
        whenever(seasons.lookup("2026", 20, null, parent)).thenReturn(
            LookupResponse(
                listOf(LookupItem(id, "2026 S1")),
                "next"
            )
        )
        mvc.get("/api/tournaments/lookup/?q=winter") {
            with(user("reader").roles("PLAYER"))
        }.andExpect {
            status { isOk() }
            jsonPath("$.items[0].id", equalTo(id.toString()))
            jsonPath("$.items[0].name", equalTo("Winter Open"))
            jsonPath("$.nextCursor") { hasJsonPath(); value(null) }
            jsonPath("$.manifest") { doesNotHaveJsonPath() }
            jsonPath("$.pagination") { doesNotHaveJsonPath() }
        }
        mvc.get("/api/seasons/lookup/?q=2026&tournamentId=$parent") {
            with(user("reader").roles("PLAYER"))
        }.andExpect {
            status { isOk() }
            jsonPath("$.items[0].name", equalTo("2026 S1"))
            jsonPath("$.nextCursor", equalTo("next"))
        }
        verifyNoInteractions(audits)
    }

    @Test
    fun `lookup retains authenticated access`() {
        mvc.get("/api/tournaments/lookup/").andExpect { status { isUnauthorized() } }
        mvc.get("/api/seasons/lookup/").andExpect { status { isUnauthorized() } }
        verifyNoInteractions(seasons, tournaments, audits)
    }
}
