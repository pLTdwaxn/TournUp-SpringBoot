package com.abeesmall.tournup.model

import com.aventrix.jnanoid.jnanoid.NanoIdUtils
import java.security.SecureRandom
import java.util.UUID

/** Internal UUIDv7 identity and an independent, opaque public locator. */
object ResourceIdentifiers {
    private val random = SecureRandom()
    const val NANO_ID_ALPHABET = "23456789BCDFGHJKLMNPQRSTVWXYZ"
    const val NANO_ID_PATTERN = "[$NANO_ID_ALPHABET]{4}"
    private val alphabet = NANO_ID_ALPHABET.toCharArray()
    private val locator = Regex("^[a-z0-9]+(?:-[a-z0-9]+)*-($NANO_ID_PATTERN)$")

    fun uuidV7(): UUID {
        val bits = UUID.randomUUID()
        return UUID(
            (System.currentTimeMillis() shl 16) or 0x7000L or
                    (bits.mostSignificantBits and 0xfffL), bits.leastSignificantBits
        )
    }

    fun nanoId(): String = NanoIdUtils.randomNanoId(random, alphabet, 4)
    fun locator(slug: String, nanoId: String): String = "$slug-$nanoId"
    fun nanoId(locator: String): String? =
        this.locator.matchEntire(locator)?.groupValues?.get(1)
}
