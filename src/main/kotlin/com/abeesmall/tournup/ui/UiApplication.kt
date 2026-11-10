package com.abeesmall.tournup.ui

const val UI_APPLICATION_HEADER = "X-TournUp-Application"

enum class UiApplication(val id: String) {
    TOURNAMENT_MANAGER("tournament-manager");

    companion object {
        fun fromHeader(value: String): UiApplication? = entries.find { it.id == value }
    }
}
