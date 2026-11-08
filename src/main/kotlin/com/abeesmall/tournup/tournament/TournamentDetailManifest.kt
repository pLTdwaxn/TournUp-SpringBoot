package com.abeesmall.tournup.tournament

import com.abeesmall.tournup.ui.UiLayout
import com.abeesmall.tournup.ui.UiLayoutType
import com.abeesmall.tournup.ui.UiManifest
import com.abeesmall.tournup.ui.UiOperation
import com.abeesmall.tournup.ui.UiSection

fun tournamentDetailManifest(title: String): UiManifest =
    tournamentReadManifest(
        id = "tournament-detail",
        operation = UiOperation.READ,
        title = title,
        layout = UiLayout(
            type = UiLayoutType.DETAIL,
            sections = listOf(
                UiSection(
                    id = "details",
                    title = "Details",
                    fields = listOf("name", "slug", "description"),
                ),
                UiSection(
                    id = "format",
                    title = "Format",
                    fields = listOf(
                        "participantType",
                        "participationMode",
                        "handicapMode",
                    ),
                ),
            ),
        ),
    )
