package com.abeesmall.tournup.service

import com.abeesmall.tournup.dto.ui.LookupItem
import com.abeesmall.tournup.dto.ui.LookupResponse
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.DataInputStream
import java.io.DataOutputStream
import java.util.Base64
import java.util.Locale
import java.util.UUID
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Sort
import org.springframework.http.HttpStatus
import org.springframework.web.server.ResponseStatusException

fun resourcePage(page: Int, size: Int, sort: Sort): PageRequest {
    if (page < 0) throw ResponseStatusException(HttpStatus.BAD_REQUEST, "page must be zero or greater")
    if (size !in 1..100) throw ResponseStatusException(HttpStatus.BAD_REQUEST, "size must be between 1 and 100")
    return PageRequest.of(page, size, sort)
}

class LookupQuery private constructor(
    private val scope: String,
    private val search: String,
    val limit: Int,
    val afterName: String?,
    val afterId: UUID?,
) {
    val pattern: String = "%" + search.replace("!", "!!").replace("%", "!%").replace("_", "!_") + "%"
    val pageable: PageRequest = PageRequest.of(0, limit + 1)

    fun response(rows: List<LookupItem>): LookupResponse {
        val items = rows.take(limit)
        val next = if (rows.size > limit) {
            val last = items.last()
            val bytes = ByteArrayOutputStream()
            DataOutputStream(bytes).use {
                it.writeInt(1)
                it.writeUTF(scope)
                it.writeUTF(search)
                it.writeUTF(last.name)
                it.writeLong(last.id.mostSignificantBits)
                it.writeLong(last.id.leastSignificantBits)
            }
            Base64.getUrlEncoder().withoutPadding().encodeToString(bytes.toByteArray())
        } else null
        return LookupResponse(items, next)
    }

    companion object {
        fun parse(scope: String, q: String, limit: Int, cursor: String?): LookupQuery {
            if (limit !in 1..100) throw ResponseStatusException(HttpStatus.BAD_REQUEST, "limit must be between 1 and 100")
            val search = q.trim().lowercase(Locale.ROOT)
            if (search.length > 255) throw ResponseStatusException(HttpStatus.BAD_REQUEST, "search must be at most 255 characters")
            if (cursor == null) return LookupQuery(scope, search, limit, null, null)
            try {
                require(cursor.length <= 4096)
                DataInputStream(ByteArrayInputStream(Base64.getUrlDecoder().decode(cursor))).use {
                    require(it.readInt() == 1 && it.readUTF() == scope && it.readUTF() == search)
                    val name = it.readUTF()
                    val id = UUID(it.readLong(), it.readLong())
                    require(it.available() == 0)
                    return LookupQuery(scope, search, limit, name, id)
                }
            } catch (failure: Exception) {
                throw ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid lookup cursor", failure)
            }
        }
    }
}
