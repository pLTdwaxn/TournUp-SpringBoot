package com.abeesmall.tournup.ui

import com.fasterxml.jackson.annotation.JsonValue

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

data class UiLayout(
    val type: UiLayoutType,
    val sections: List<UiSection> = emptyList(),
    val columns: List<String> = emptyList(),
)

data class UiSection(
    val id: String,
    val title: String,
    val fields: List<String>,
)

data class UiField(
    val label: String,
    val dataType: UiDataType,
    val widget: UiWidget,
    val required: Boolean = false,
    val editable: Boolean = true,
    val options: List<UiOption> = emptyList(),
    val behaviours: List<UiBehaviour> = emptyList(),
    val maxLength: Int? = null,
    val pattern: String? = null,
)

data class UiOption(
    val value: String,
    val label: String,
)

data class UiBehaviour(
    val type: UiBehaviourType,
    val sourceField: String,
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
}

enum class UiWidget(
    @get:JsonValue val wireValue: String,
) {
    TEXT("text"),
    TEXTAREA("textarea"),
    SELECT("select"),
    DISPLAY("display"),
    NUMBER("number"),
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
