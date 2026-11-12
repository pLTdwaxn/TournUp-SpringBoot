package com.abeesmall.tournup.ui

import com.abeesmall.tournup.ui.resource.UiResourceRegistration
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import org.springframework.web.util.UriComponentsBuilder

data class UiManifestContext(
    val application: UiApplication,
    val operation: UiOperation,
    val allowedWrites: Set<UiOperation> = emptySet(),
    val title: String? = null,
    val recordId: String? = null,
    val relatedIds: Map<String, String> = emptyMap(),
)

data class UiManifestCommand(
    val requiredWrite: UiOperation,
    val id: String,
    val label: String,
    val operationId: String,
    val queryParameters: List<String> = emptyList(),
)

data class UiViewDefinition(
    val id: String,
    val title: String,
    val layout: UiLayout,
    val fieldIds: List<String>,
    val widgets: Map<String, UiWidget> = emptyMap(),
    val behaviours: Map<String, List<UiBehaviour>> = emptyMap(),
    val commands: List<UiManifestCommand> = emptyList(),
)

@Component
class UiManifestResolver(
    private val projector: UiConstraintProjector,
    private val layouts: PublishedUiLayoutReader? = null,
) {
    private val logger = LoggerFactory.getLogger(UiManifestResolver::class.java)

    fun fields(definition: UiResourceRegistration, operation: UiOperation = UiOperation.CREATE) =
        projector.project(if (operation == UiOperation.UPDATE) definition.definition.update else definition.definition.create,
            definition.presentation)

    fun resolve(definition: UiResourceRegistration, context: UiManifestContext): UiManifest {
        require(context.application in definition.applications) { "Unsupported UI application" }
        val resource = definition.definition.key
        val view = definition.views.getValue(context.operation)
        val form = view.layout.type == UiLayoutType.FORM
        val contract = when (context.operation) {
            UiOperation.CREATE -> definition.definition.create
            UiOperation.UPDATE -> definition.definition.update
            else -> null
        }
        val definitions = fields(definition, context.operation)
        val fields = view.fieldIds.associateWith { id ->
            val field = definitions.getValue(id)
            val writable = form && context.operation in context.allowedWrites &&
                    contract?.writable(id) == true
            val widget = view.widgets[id] ?: if (!form) UiWidget.DISPLAY else when (field.dataType) {
                UiDataType.STRING -> UiWidget.TEXT
                UiDataType.INTEGER -> UiWidget.NUMBER
                UiDataType.ENUM -> UiWidget.SELECT
                UiDataType.REFERENCE -> UiWidget.RECORD_SELECT
            }
            field.toUiField(widget, writable, if (writable) view.behaviours[id].orEmpty() else emptyList()).let {
                if (form) it else it.copy(required = null, maxLength = null, pattern = null)
            }
        }
        val placed = view.layout.columns + view.layout.sections.flatMap { it.fields }
        require(placed.all { it in fields }) { "Layout refers to an unavailable field" }
        for (field in fields.values) {
            for (behaviour in field.behaviours) {
                require(behaviour.dependsOn.all { it in fields }) { "Unknown behaviour dependency" }
            }
        }
        val manifest = UiManifest(
            contractVersion = 1, id = view.id, application = context.application.id,
            resource = resource, operation = context.operation, title = context.title ?: view.title,
            layout = view.layout, fields = fields,
            commands = view.commands.filter {
                it.requiredWrite in context.allowedWrites &&
                    definition.supports(definition.operations.single { operation -> operation.id == it.operationId })
            }.map { command ->
                val operation = definition.operations.single { it.id == command.operationId }
                val values = context.relatedIds + (context.recordId?.let { mapOf("id" to it) } ?: emptyMap())
                val path = Regex("\\{([a-zA-Z][a-zA-Z0-9]*)}").replace(operation.href) { match ->
                    requireNotNull(values[match.groupValues[1]]) { "Missing operation context: ${match.groupValues[1]}" }
                }
                val builder = UriComponentsBuilder.fromUriString(path)
                command.queryParameters.forEach { parameter ->
                    values[parameter]?.let {
                        builder.queryParam(
                            parameter,
                            it
                        )
                    }
                }
                UiCommand(command.id, command.label, operation.method, builder.build().encode().toUriString())
            },
        )
        return if (context.operation in definition.publishedViews) published(manifest) else manifest
    }

    private fun published(base: UiManifest): UiManifest {
        val published = layouts?.find(base.application, base.resource, base.operation.wireValue) ?: return base
        val definition = published.definition
        val validation = validateLayoutDefinition(definition, base.fields.mapValues { it.value.dataType }, emptySet())
        if (published.contractVersion != base.contractVersion ||
            definition.layout.type != UiLayoutType.TABLE || !validation.isValid) {
            logger.warn("Ignoring invalid {} list layout {} revision {} for application {}",
                base.resource, published.layoutId, published.revision, base.application)
            return base
        }
        return base.copy(title = definition.title, layout = definition.layout,
            fields = base.fields.mapValues { (id, field) ->
                definition.fields[id]?.let { field.copy(label = it.label, widget = it.widget) } ?: field
            })
    }
}
