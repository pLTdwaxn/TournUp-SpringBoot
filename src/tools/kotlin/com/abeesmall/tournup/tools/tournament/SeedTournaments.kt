package com.abeesmall.tournup.tools.tournament

import com.abeesmall.tournup.model.tournament.HandicapMode
import com.abeesmall.tournup.model.tournament.ParticipantType
import com.abeesmall.tournup.model.tournament.ParticipationMode
import java.nio.file.Files
import java.nio.file.Path
import java.sql.DriverManager
import java.util.Properties
import com.abeesmall.tournup.model.ResourceIdentifiers

fun main() {
    val target = 41
    val database = "demo_club_1_tournup"

    val properties = Properties()
    val envFile = Path.of(".env")
    if (Files.isRegularFile(envFile)) {
        Files.newBufferedReader(envFile).use { properties.load(it) }
    }

    fun setting(key: String): String? =
        System.getenv(key) ?: properties.getProperty(key)

    val port = (setting("DB_PORT") ?: "5432").toInt()
    require(port in 1..65535)

    val password = setting("DEMO_TENANT_1_DB_PASSWORD")
        ?.takeIf { it.isNotEmpty() }
        ?: error("Set DEMO_TENANT_1_DB_PASSWORD in .env or the environment")

    DriverManager.getConnection(
        "jdbc:postgresql://127.0.0.1:$port/$database",
        "demo_club_1_app",
        password,
    ).use { connection ->
        connection.autoCommit = false

        try {
            val existingSlugs = mutableSetOf<String>()
            connection.createStatement().use { statement ->
                statement.executeQuery(
                    "SELECT slug FROM public.tournaments",
                ).use { rows ->
                    while (rows.next()) {
                        existingSlugs.add(rows.getString(1))
                    }
                }
            }

            check(existingSlugs.size <= target) {
                "Already ${existingSlugs.size} tournaments; target is $target"
            }

            val remaining = target - existingSlugs.size
            val candidates = (1..target)
                .filter { dummySlug(it) !in existingSlugs }
                .take(remaining)

            connection.prepareStatement(
                """
                INSERT INTO public.tournaments (
                    id, slug, name, description,
                    handicap_mode, participation_mode, participant_type, nano_id
                )
                VALUES (?, ?, ?, ?, ?, ?, ?, ?)
                ON CONFLICT (slug) DO NOTHING
                """.trimIndent(),
            ).use { statement ->
                for (n in candidates) {
                    statement.setObject(1, ResourceIdentifiers.uuidV7())
                    statement.setString(8, ResourceIdentifiers.nanoId())
                    statement.setString(2, dummySlug(n))
                    statement.setString(
                        3,
                        "Demo Club 1 Tournament ${n.toString().padStart(2, '0')}",
                    )
                    statement.setString(
                        4,
                        if (n % 5 == 0) null else "Dummy tournament for local development.",
                    )
                    statement.setString(
                        5,
                        HandicapMode.entries[(n - 1) % HandicapMode.entries.size].name,
                    )
                    statement.setString(
                        6,
                        ParticipationMode.entries[
                            ((n - 1) / 2) % ParticipationMode.entries.size
                        ].name,
                    )
                    statement.setString(
                        7,
                        ParticipantType.entries[(n - 1) % ParticipantType.entries.size].name,
                    )
                    statement.addBatch()
                }
                if (candidates.isNotEmpty()) {
                    statement.executeBatch()
                }
            }

            val total = connection.createStatement().use { statement ->
                statement.executeQuery(
                    "SELECT count(*) FROM public.tournaments",
                ).use { rows ->
                    check(rows.next())
                    rows.getInt(1)
                }
            }

            check(total == target) {
                "Expected $target tournaments, found $total"
            }
            connection.commit()
            println("$database: $total tournaments. Seed complete.")
        } catch (failure: Exception) {
            connection.rollback()
            throw failure
        }
    }
}

private fun dummySlug(number: Int): String =
    "dummy-club-1-${number.toString().padStart(2, '0')}"
