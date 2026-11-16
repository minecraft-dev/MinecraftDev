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

package com.demonwav.mcdev.util

import com.demonwav.mcdev.update.PluginUtil
import com.intellij.util.io.HttpRequests
import com.intellij.util.io.RequestBuilder
import java.net.URI
import java.net.URISyntaxException
import kotlin.collections.iterator
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * HTTP util.
 *
 * When the [MOCK_PROPERTY] system property is set to the base URL of a mock server (for example
 * `http://127.0.0.1:54321`), [resolve] rewrites `https://host/path?query` to `<base>/host/path?query` so tests can serve
 * recorded responses.
 */
object McdevHttp {
    const val MOCK_PROPERTY = "mcdev.test.httpMock"

    val mockBaseUrl: String?
        get() = System.getProperty(MOCK_PROPERTY)?.trim()?.trimEnd('/')?.takeIf { it.isNotEmpty() }

    fun resolve(url: String): String {
        val base = mockBaseUrl ?: return url
        return rewrite(url, base)
    }

    internal fun rewrite(url: String, base: String): String {
        val uri = try {
            URI(url)
        } catch (_: URISyntaxException) {
            return url
        }
        val scheme = uri.scheme?.lowercase()
        if (scheme != "http" && scheme != "https") {
            return url
        }
        val host = uri.host ?: return url

        val authority = if (uri.port == -1) host else "$host:${uri.port}"
        val path = uri.rawPath.orEmpty()
        val query = uri.rawQuery?.let { "?$it" }.orEmpty()
        return "$base/$authority$path$query"
    }

    fun request(url: String, headers: Map<String, String> = emptyMap()): RequestBuilder =
        build(url, headers) { HttpRequests.request(it) }

    fun post(url: String, contentType: String, headers: Map<String, String> = emptyMap()): RequestBuilder =
        build(url, headers) { HttpRequests.post(it, contentType) }

    private inline fun build(
        url: String,
        headers: Map<String, String>,
        create: (String) -> RequestBuilder,
    ): RequestBuilder {
        val resolved = resolve(url)
        val builder = create(resolved).userAgent(PluginUtil.useragent)
        if (resolved != url) {
            builder.useProxy(false)
        }
        if (headers.isNotEmpty()) {
            val host = try {
                URI(resolved).host
            } catch (_: URISyntaxException) {
                null
            }
            // HttpRequests runs the tuner again for every redirect it follows
            builder.tuner { connection ->
                if (connection.url.host.equals(host, ignoreCase = true)) {
                    for ((name, value) in headers) {
                        connection.setRequestProperty(name, value)
                    }
                }
            }
        }
        return builder
    }

    /**
     * Convenience version of [request].
     */
    fun get(
        url: String,
        headers: Map<String, String> = emptyMap(),
        configure: RequestBuilder.() -> Unit = {},
    ): Request {
        val request = request(url, headers)
        request.configure()
        return Request(request)
    }

    suspend fun getString(url: String, headers: Map<String, String> = emptyMap()): String =
        get(url, headers).suspend().connect { readString() }

    class Request(private val builder: RequestBuilder) {
        fun <T> connect(read: HttpRequests.Request.() -> T): T {
            return builder.connect { conn -> conn.read() }
        }

        fun suspend(): SuspendingRequest = SuspendingRequest(this)
    }

    class SuspendingRequest(private val request: Request) {
        suspend fun <T> connect(read: HttpRequests.Request.() -> T): T = withContext(Dispatchers.IO) {
            request.connect(read)
        }
    }
}
