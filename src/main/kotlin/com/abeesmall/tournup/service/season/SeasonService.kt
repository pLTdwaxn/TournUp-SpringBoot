package com.abeesmall.tournup.service.season

import com.abeesmall.tournup.dto.season.SeasonData
import com.abeesmall.tournup.dto.season.SeasonNamingContext
import com.abeesmall.tournup.dto.season.SeasonWriteRequest
import com.abeesmall.tournup.dto.ui.LookupResponse
import com.abeesmall.tournup.dto.ui.UiCollectionResponse
import com.abeesmall.tournup.dto.ui.UiRecordResponse
import com.abeesmall.tournup.manifest.season.seasonResourceDefinition
import com.abeesmall.tournup.model.season.Season
import com.abeesmall.tournup.model.tournament.Tournament
import com.abeesmall.tournup.repository.season.SeasonRepository
import com.abeesmall.tournup.repository.tournament.TournamentRepository
import com.abeesmall.tournup.service.LookupQuery
import com.abeesmall.tournup.service.RecordContextRequiredException
import com.abeesmall.tournup.service.parseRecordId
import com.abeesmall.tournup.service.toUiResponse
import com.abeesmall.tournup.service.tournament.includedReference
import com.abeesmall.tournup.ui.UiApplication
import com.abeesmall.tournup.ui.UiManifestContext
import com.abeesmall.tournup.ui.UiManifestResolver
import com.abeesmall.tournup.ui.UiOperation
import jakarta.validation.ConstraintViolationException
import jakarta.validation.Validator
import java.util.UUID
import org.springframework.dao.DataIntegrityViolationException
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional


@Service
class SeasonService(
    private val tournaments: TournamentRepository,
    private val seasons: SeasonRepository,
    private val validator: Validator,
) {

    @Transactional(readOnly = true)
    fun lookup(q: String, limit: Int, cursor: String?, tournamentId: UUID? = null): LookupResponse {
        if (tournamentId != null) requireTournament(tournamentId)
        val query = LookupQuery.parse("season:" + (tournamentId?.toString() ?: "all"), q, limit, cursor)
        return query.response(seasons.lookup(
            query.pattern, query.afterName, query.afterId, tournamentId, query.pageable,
        ))
    }

    @Transactional(readOnly = true)
    fun form(application: UiApplication, tournamentId: UUID?): UiRecordResponse<SeasonWriteRequest> {
        val manifest = UiManifestResolver.resolve(
            seasonResourceDefinition(),
            UiManifestContext(
                application,
                UiOperation.CREATE,
                allowedWrites = setOf(UiOperation.CREATE),
            ),
        )
        if (tournamentId == null) return UiRecordResponse(manifest, item = SeasonWriteRequest("", "", null, null))
        val tournament = requireTournament(tournamentId)
        return UiRecordResponse(
            manifest = manifest,
            item = SeasonWriteRequest("", "", null, tournament.id),
            included = tournament.includedReference(),
        )
    }

    @Transactional(readOnly = true)
    fun namingContext(tournamentId: UUID): SeasonNamingContext {
        requireTournament(tournamentId)
        val existing = seasons.findAllByTournamentIdOrderByFromYearAscNameAsc(tournamentId)

        return SeasonNamingContext(
            tournamentId = tournamentId,
            existingNamesByYear = existing.groupBy(
                keySelector = { it.fromYear },
                valueTransform = { it.name },
            ),
        )
    }

    @Transactional
    fun create(request: SeasonWriteRequest): UiRecordResponse<SeasonData> {
        val violations = validator.validate(request)
        if (violations.isNotEmpty()) {
            throw ConstraintViolationException(violations)
        }

        val tournamentId = requireNotNull(request.tournamentId)
        val tournament = requireTournament(tournamentId)
        if (seasons.existsByTournamentIdAndName(tournamentId, request.name)) {
            throw SeasonFieldConflictException("name")
        }
        if (seasons.existsByTournamentIdAndSlug(tournamentId, request.slug)) {
            throw SeasonFieldConflictException("slug")
        }

        val season = try {
            seasons.saveAndFlush(
                Season(
                    tournament = tournament,
                    name = request.name,
                    slug = request.slug,
                    fromYear = requireNotNull(request.fromYear),
                ),
            )
        } catch (failure: DataIntegrityViolationException) {
            throw seasonFieldConflict(failure) ?: failure
        }

        return seasonDetailResponse(season, canUpdate = true)
    }

    private fun requireTournament(tournamentId: UUID): Tournament =
        tournaments.findById(tournamentId).orElseThrow {
            SeasonTournamentNotFoundException(tournamentId)
        }

    @Transactional(readOnly = true)
    fun find(
        identifier: String,
        tournamentIdentifier: String? = null,
        canUpdate: Boolean = false,
    ): UiRecordResponse<SeasonData>? {
        val id = parseRecordId(identifier)
        if (id == null && tournamentIdentifier == null) {
            throw RecordContextRequiredException("Tournament")
        }
        val tournamentId = tournamentIdentifier?.let { parent ->
            parseRecordId(parent) ?: tournaments.findBySlug(parent)?.id ?: return null
        }
        val season = if (id != null) {
            seasons.findById(id).orElse(null)
        } else {
            seasons.findByTournamentIdAndSlug(requireNotNull(tournamentId), identifier)
        } ?: return null
        val tournament = season.tournament
        if (tournamentId != null && tournament.id != tournamentId) {
            return null
        }

        return seasonDetailResponse(season, canUpdate)
    }

    @Transactional(readOnly = true)
    fun list(
        tournamentId: UUID?,
        pageable: Pageable,
        canCreate: Boolean = false,
    ): UiCollectionResponse<SeasonData> {
        if (tournamentId != null && !tournaments.existsById(tournamentId)) {
            throw SeasonTournamentNotFoundException(tournamentId)
        }
        val result = if (tournamentId == null) {
            seasons.findAll(pageable)
        } else {
            seasons.findAllByTournamentId(tournamentId, pageable)
        }

        return result.toUiResponse(
            manifest = UiManifestResolver.resolve(
                seasonResourceDefinition(),
                UiManifestContext(
                    UiApplication.TOURNAMENT_MANAGER,
                    UiOperation.LIST,
                    allowedWrites = if (canCreate) setOf(UiOperation.CREATE) else emptySet(),
                    relatedIds = tournamentId?.let { mapOf("tournamentId" to it.toString()) } ?: emptyMap(),
                ),
            ),
            transform = { it.toData() },
        )
    }

    @Transactional(readOnly = true)
    fun form(id: UUID, tournamentId: UUID? = null): UiRecordResponse<SeasonWriteRequest>? {
        val season = seasons.findById(id).orElse(null) ?: return null
        if (tournamentId != null && season.tournament.id != tournamentId) {
            return null
        }
        return UiRecordResponse(
            manifest = UiManifestResolver.resolve(
                seasonResourceDefinition(),
                UiManifestContext(
                    UiApplication.TOURNAMENT_MANAGER,
                    UiOperation.UPDATE,
                    allowedWrites = setOf(UiOperation.UPDATE),
                    recordId = season.id.toString(),
                ),
            ),
            item = SeasonWriteRequest(season.name, season.slug, season.fromYear, season.tournament.id),
            included = season.tournament.includedReference(),
        )
    }

    @Transactional
    fun update(
        id: UUID,
        request: SeasonWriteRequest,
        tournamentId: UUID? = null,
    ): UiRecordResponse<SeasonData>? {
        val violations = validator.validate(request)
        if (violations.isNotEmpty()) {
            throw ConstraintViolationException(violations)
        }
        val season = seasons.findById(id).orElse(null) ?: return null
        val actualTournamentId = season.tournament.id
        if (tournamentId != null && actualTournamentId != tournamentId) {
            return null
        }
        if (request.tournamentId != actualTournamentId) {
            throw SeasonTournamentChangeException()
        }

        if (seasons.existsByTournamentIdAndNameAndIdNot(actualTournamentId, request.name, id)) {
            throw SeasonFieldConflictException("name")
        }
        if (seasons.existsByTournamentIdAndSlugAndIdNot(actualTournamentId, request.slug, id)) {
            throw SeasonFieldConflictException("slug")
        }

        season.name = request.name
        season.slug = request.slug
        season.fromYear = requireNotNull(request.fromYear)
        val saved = try {
            seasons.saveAndFlush(season)
        } catch (failure: DataIntegrityViolationException) {
            throw seasonFieldConflict(failure) ?: failure
        }

        return seasonDetailResponse(saved, canUpdate = true)
    }
}
internal fun seasonDetailResponse(season: Season, canUpdate: Boolean): UiRecordResponse<SeasonData> =
    UiRecordResponse(
        manifest = UiManifestResolver.resolve(
            seasonResourceDefinition(),
            UiManifestContext(
                UiApplication.TOURNAMENT_MANAGER,
                UiOperation.READ,
                title = season.name,
                allowedWrites = if (canUpdate) setOf(UiOperation.UPDATE) else emptySet(),
                recordId = season.id.toString(),
            ),
        ),
        item = season.toData(),
        included = season.tournament.includedReference(),
    )
