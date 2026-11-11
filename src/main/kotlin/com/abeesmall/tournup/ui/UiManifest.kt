package com.abeesmall.tournup.ui

import com.fasterxml.jackson.annotation.JsonValue
import com.fasterxml.jackson.annotation.JsonInclude

data class UiManifest(
    val contractVersion: Int,
    val id: String,
    val application: String,
    val resource: String,
    val operation: UiOperation,
    val title: String,
    val layout: UiLayout,
    val fields: Map<String, UiField>,
    val commands: List<UiCommand>,
)

// Absent sections or columns mean an empty list for that layout property.
data class UiLayout(
    val type: UiLayoutType,
    @get:JsonInclude(JsonInclude.Include.NON_EMPTY)
    val sections: List<UiSection> = emptyList(),
    @get:JsonInclude(JsonInclude.Include.NON_EMPTY)
    val columns: List<String> = emptyList(),
)

data class UiSection(
    val id: String,
    val title: String,
    val fields: List<String>,
)

// Absent options/behaviours mean empty lists. Required is emitted only for forms;
// nullable constraints and reference metadata are omitted when inapplicable.
@JsonInclude(JsonInclude.Include.NON_NULL)
data class UiField(
    val label: String,
    val dataType: UiDataType,
    val widget: UiWidget,
    val required: Boolean? = null,
    val editable: Boolean = true,
    @get:JsonInclude(JsonInclude.Include.NON_EMPTY)
    val options: List<UiOption> = emptyList(),
    @get:JsonInclude(JsonInclude.Include.NON_EMPTY)
    val behaviours: List<UiBehaviour> = emptyList(),
    val maxLength: Int? = null,
    val pattern: String? = null,
    val reference: UiReference? = null,
)

data class UiOption(
    val value: String,
    val label: String,
)

@JsonInclude(JsonInclude.Include.NON_NULL)
data class UiBehaviour(
    val type: UiBehaviourType,
    val dependsOn: List<String>,
    val contextOperation: String? = null,
)

data class UiReference(
    val resource: String,
    val lookupOperation: String,
    val resolveOperation: String,
)

data class UiCommand(
    val id: String,
    val label: String,
    val method: UiCommandMethod,
    val href: String,
)

enum class UiOperation(
    @get:JsonValue val wireValue: String,
) {
    CREATE("create"),
    LIST("list"),
    READ("read"),
    UPDATE("update"),
}

enum class UiLayoutType(
    @get:JsonValue val wireValue: String,
) {
    FORM("form"),
    TABLE("table"),
    DETAIL("detail"),
}

enum class UiDataType(
    @get:JsonValue val wireValue: String,
) {
    STRING("string"),
    ENUM("enum"),
    INTEGER("integer"),
    REFERENCE("reference"),
}

enum class UiWidget(
    @get:JsonValue val wireValue: String,
) {
    TEXT("text"),
    TEXTAREA("textarea"),
    SELECT("select"),
    DISPLAY("display"),
    NUMBER("number"),
    RECORD_SELECT("record-select"),
}

enum class UiBehaviourType(
    @get:JsonValue val wireValue: String,
) {
    SLUGIFY("slugify"),
    SEASON_NAME_FROM_YEAR("season-name-from-year"),
}

enum class UiCommandMethod(
    @get:JsonValue val wireValue: String,
) {
    GET("GET"),
    POST("POST"),
    PUT("PUT"),
}
