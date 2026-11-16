/*
 * Minecraft Development for IntelliJ
 *
 * https://mcdev.io/
 *
 * Copyright (C) 2026 minecraft-dev
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published
 * by the Free Software Foundation, version 3.0 only.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package com.demonwav.mcdev.framework.http

import java.net.URI
import java.net.URISyntaxException
import java.security.MessageDigest

/**
 * Maps URLs to the file names of their WireMock mapping and body file. Re-recording a URL overwrites the same files.
 *
 * The path is `<host>/<path segments>`, where:
 *
 * - The scheme is dropped, so `http` and `https` URLs share a fixture.
 * - The host is lowercased. A non-default port is appended as `%3A<port>`.
 * - Each path segment keeps its raw (already percent-encoded) form. Characters that aren't safe in file names on every
 *   OS are percent-encoded on top of that.
 * - The query string is appended to the last segment as `%3F<query>`, encoded the same way.
 * - A URL that ends in `/`, or has no path, is stored as [INDEX_NAME] inside that directory.
 * - Names longer than [MAX_NAME_LENGTH] are shortened and suffixed with a hash of the full name.
 *
 * The fragment is ignored.
 */
object HttpFixturePaths {
    const val INDEX_NAME = "_index"
    const val MAX_NAME_LENGTH = 120

    private const val HASH_LENGTH = 16
    private val windowsReservedNames = setOf("con", "prn", "aux", "nul") +
        (1..9).flatMap { listOf("com$it", "lpt$it") }

    /**
     * Returns the fixture path for [url] as a list of path segments, or throws [IllegalArgumentException] for anything
     * that isn't an absolute HTTP(S) URL.
     */
    fun segmentsFor(url: String): List<String> {
        val uri = parse(url)
        val host = requireNotNull(uri.host) { "URL has no host: $url" }.lowercase()

        val result = mutableListOf(encodeName(if (uri.port == -1) host else "$host:${uri.port}"))

        val path = uri.rawPath.orEmpty().removePrefix("/")
        val pathSegments = if (path.isEmpty()) mutableListOf("") else path.split('/').toMutableList()
        if (pathSegments.last().isEmpty()) {
            pathSegments[pathSegments.lastIndex] = INDEX_NAME
        }

        val query = uri.rawQuery
        for ((index, segment) in pathSegments.withIndex()) {
            val isLast = index == pathSegments.lastIndex
            val raw = if (isLast && query != null) "$segment?$query" else segment
            result += encodeName(raw)
        }
        return result
    }

    /**
     * The fixture path for [url], joined with `/`.
     */
    fun pathFor(url: String): String = segmentsFor(url).joinToString("/")

    /**
     * The path and query the mock server receives for [url], `/<host>/<path>?<query>`. This matches how
     * `McdevHttp` rewrites URLs, and is what fixture mappings match on.
     */
    fun mockPath(url: String): String {
        val uri = parse(url)
        val host = requireNotNull(uri.host) { "URL has no host: $url" }
        val authority = if (uri.port == -1) host else "$host:${uri.port}"
        val query = uri.rawQuery?.let { "?$it" }.orEmpty()
        return "/$authority${uri.rawPath.orEmpty()}$query"
    }

    /**
     * Returns the URL a request to [mockPath] was meant for, or null if [mockPath] has no host. `http` URLs come back
     * as `https`.
     */
    fun originalUrl(mockPath: String): String? =
        mockPath.removePrefix("/").takeIf { it.isNotEmpty() }?.let { "https://$it" }

    private fun parse(url: String): URI {
        val uri = try {
            URI(url)
        } catch (e: URISyntaxException) {
            throw IllegalArgumentException("Not a valid URL: $url", e)
        }
        val scheme = uri.scheme?.lowercase()
        require(scheme == "http" || scheme == "https") { "Not an HTTP URL: $url" }
        return uri
    }

    private fun encodeName(raw: String): String {
        val encoded = StringBuilder()
        for (byte in raw.toByteArray(Charsets.UTF_8)) {
            val unsigned = byte.toUByte()
            val c = Char(unsigned.toUShort())
            if (unsigned < 0x80u && isSafe(c)) {
                encoded.append(c)
            } else {
                encoded.append('%').append(unsigned.toHexString(HexFormat.UpperCase))
            }
        }

        // ".", "..", and names Windows won't create
        val dotHexed = "%" + '.'.code.toUByte().toHexString(HexFormat.UpperCase)
        var name = encoded.toString()
        if (name == "." || name == "..") {
            name = name.replace(".", dotHexed)
        }
        if (name.endsWith('.')) {
            name = name.dropLast(1) + dotHexed
        }
        if (name.substringBefore('.').lowercase() in windowsReservedNames) {
            name = "%" + name[0].code.toUByte().toHexString(HexFormat.UpperCase) + name.substring(1)
        }

        if (name.length > MAX_NAME_LENGTH) {
            val digest = MessageDigest.getInstance("SHA-256").digest(name.toByteArray(Charsets.UTF_8))
            val hash = digest.toHexString().take(HASH_LENGTH)
            name = name.take(MAX_NAME_LENGTH - HASH_LENGTH - 1) + "~" + hash
        }
        return name
    }

    // '%' is kept because raw URL parts are already percent-encoded
    private fun isSafe(c: Char): Boolean =
        c in 'a'..'z' || c in 'A'..'Z' || c in '0'..'9' || c in "-._~@+,=&%!$'()"
}
