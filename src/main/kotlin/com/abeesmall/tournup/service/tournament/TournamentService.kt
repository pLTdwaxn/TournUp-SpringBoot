package com.abeesmall.tournup.service.tournament

import com.abeesmall.tournup.dto.tournament.TournamentCreateRequest
import com.abeesmall.tournup.dto.tournament.TournamentData
import com.abeesmall.tournup.dto.tournament.TournamentUpdateRequest
import com.abeesmall.tournup.dto.ui.LookupResponse
import com.abeesmall.tournup.dto.ui.UiCollectionResponse
import com.abeesmall.tournup.dto.ui.UiRecordResponse
import com.abeesmall.tournup.manifest.tournament.tournamentResourceDefinition
import com.abeesmall.tournup.model.tournament.Tournament
import com.abeesmall.tournup.repository.tournament.TournamentRepository
import com.abeesmall.tournup.service.LookupQuery
import com.abeesmall.tournup.service.parseRecordId
import com.abeesmall.tournup.service.toUiResponse
import com.abeesmall.tournup.ui.PublishedUiLayoutReader
import com.abeesmall.tournup.ui.UiApplication
import com.abeesmall.tournup.ui.UiLayoutType
import com.abeesmall.tournup.ui.UiManifest
import com.abeesmall.tournup.ui.UiManifestContext
import com.abeesmall.tournup.ui.UiManifestResolver
import com.abeesmall.tournup.ui.UiOperation
import com.abeesmall.tournup.ui.validateLayoutDefinition
import java.util.UUID
import org.hibernate.exception.ConstraintViolationException
import org.slf4j.LoggerFactory
import org.springframework.dao.DataIntegrityViolationException
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional


@Service
class TournamentService(
    private val tournaments: TournamentRepository,
    private val layouts: PublishedUiLayoutReader,
) {

    @Transactional(readOnly = true)
    fun lookup(q: String, limit: Int, cursor: String?): LookupResponse {
        val query = LookupQuery.parse("tournament", q, limit, cursor)
        return query.response(tournaments.lookup(
            query.pattern, query.afterName, query.afterId, query.pageable,
        ))
    }

    @Transactional
    fun create(request: TournamentCreateRequest): UiRecordResponse<TournamentData> {
        if (tournaments.findBySlug(request.slug) != null) {
            throw TournamentSlugConflictException()
        }

        val tournament = try {
            tournaments.saveAndFlush(
                Tournament(
                    slug = request.slug,
                    name = request.name,
                    description = request.description,
                    participantType = request.participantType,
                    participationMode = request.participationMode,
                    handicapMode = request.handicapMode,
                ),
            )
        } catch (failure: DataIntegrityViolationException) {
            val slugConflict = generateSequence<Throwable>(failure) { it.cause }
                .filterIsInstance<ConstraintViolationException>()
                .any { it.constraintName == "tournaments_slug_unique" }

            if (slugConflict) {
                throw TournamentSlugConflictException(failure)
            }
            throw failure
        }

        return UiRecordResponse(
            manifest = UiManifestResolver.resolve(
                tournamentResourceDefinition(),
                UiManifestContext(
                    UiApplication.TOURNAMENT_MANAGER,
                    UiOperation.READ,
                    title = tournament.name,
                    allowedWrites = setOf(UiOperation.UPDATE),
                    recordId = tournament.id.toString(),
                ),
            ),
            item = tournament.toData(),
        )
    }

    @Transactional(readOnly = true)
    fun find(
        identifier: String,
        canUpdate: Boolean = false,
    ): UiRecordResponse<TournamentData>? {
        val id = parseRecordId(identifier)
        val tournament = if (id != null) {
            tournaments.findById(id).orElse(null)
        } else {
            tournaments.findBySlug(identifier)
        } ?: return null

        return UiRecordResponse(
            manifest = UiManifestResolver.resolve(
                tournamentResourceDefinition(),
                UiManifestContext(
                    UiApplication.TOURNAMENT_MANAGER,
                    UiOperation.READ,
                    title = tournament.name,
                    allowedWrites = if (canUpdate) setOf(UiOperation.UPDATE) else emptySet(),
                    recordId = tournament.id.toString(),
                ),
            ),
            item = tournament.toData(),
        )
    }

    private val logger = LoggerFactory.getLogger(TournamentService::class.java)

    @Transactional(readOnly = true)
    fun list(
        pageable: Pageable,
        application: UiApplication,
        canCreate: Boolean = false,
    ): UiCollectionResponse<TournamentData> {
        val result = tournaments.findAll(pageable)

        return result.toUiResponse(
            manifest = listManifest(canCreate, application),
            transform = { it.toData() },
        )
    }

    private fun listManifest(canCreate: Boolean, application: UiApplication): UiManifest {
        val base = UiManifestResolver.resolve(
            tournamentResourceDefinition(),
            UiManifestContext(
                application,
                UiOperation.LIST,
                allowedWrites = if (canCreate) setOf(UiOperation.CREATE) else emptySet(),
            ),
        )
        val published = layouts.find(base.application, base.resource, base.operation.wireValue)
            ?: return base
        val definition = published.definition
        val validation = validateLayoutDefinition(
            definition,
            base.fields.mapValues { it.value.dataType },
            requiredWithoutDefaults = emptySet(),
        )
        if (published.contractVersion != base.contractVersion ||
            definition.layout.type != UiLayoutType.TABLE || !validation.isValid
        ) {
            logger.warn(
                "Ignoring invalid tournament list layout {} revision {} for application {}",
                published.layoutId, published.revision, application.id,
            )
            return base
        }

        return base.copy(
            title = definition.title,
            layout = definition.layout,
            fields = base.fields.mapValues { (id, field) ->
                definition.fields[id]?.let { presentation ->
                    field.copy(label = presentation.label, widget = presentation.widget)
                } ?: field
            },
        )
    }

    @Transactional(readOnly = true)
    fun form(id: UUID): UiRecordResponse<TournamentUpdateRequest>? {
        val tournament = tournaments.findById(id).orElse(null)
            ?: return null

        return UiRecordResponse(
            manifest = UiManifestResolver.resolve(
                tournamentResourceDefinition(),
                UiManifestContext(
                    UiApplication.TOURNAMENT_MANAGER,
                    UiOperation.UPDATE,
                    allowedWrites = setOf(UiOperation.UPDATE),
                    recordId = tournament.id.toString(),
                ),
            ),
            item = TournamentUpdateRequest(
                name = tournament.name,
                slug = tournament.slug,
                description = tournament.description,
                participantType = tournament.participantType,
                participationMode = tournament.participationMode,
                handicapMode = tournament.handicapMode,
            ),
        )
    }

    @Transactional
    fun update(
        id: UUID,
        request: TournamentUpdateRequest,
    ): UiRecordResponse<TournamentData>? {
        val tournament = tournaments.findById(id).orElse(null)
            ?: return null

        val slugOwner = tournaments.findBySlug(request.slug)
        if (slugOwner != null && slugOwner.id != id) {
            throw TournamentSlugConflictException()
        }

        tournament.name = request.name
        tournament.slug = request.slug
        tournament.description = request.description
        tournament.participantType = request.participantType
        tournament.participationMode = request.participationMode
        tournament.handicapMode = request.handicapMode

        val saved = try {
            tournaments.saveAndFlush(tournament)
        } catch (failure: DataIntegrityViolationException) {
            val slugConflict = generateSequence<Throwable>(failure) { it.cause }
                .filterIsInstance<ConstraintViolationException>()
                .any { it.constraintName == "tournaments_slug_unique" }

            if (slugConflict) {
                throw TournamentSlugConflictException(failure)
            }
            throw failure
        }

        return UiRecordResponse(
            manifest = UiManifestResolver.resolve(
                tournamentResourceDefinition(),
                UiManifestContext(
                    UiApplication.TOURNAMENT_MANAGER,
                    UiOperation.READ,
                    title = saved.name,
                    allowedWrites = setOf(UiOperation.UPDATE),
                    recordId = saved.id.toString(),
                ),
            ),
            item = saved.toData(),
        )
    }
}
