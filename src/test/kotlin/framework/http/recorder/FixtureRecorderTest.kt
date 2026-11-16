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

package com.demonwav.mcdev.framework.http.recorder

import com.demonwav.mcdev.framework.http.HttpFixtures
import com.sun.net.httpserver.HttpServer
import java.net.InetAddress
import java.net.InetSocketAddress
import java.nio.file.Path
import kotlin.io.path.exists
import kotlin.io.path.readText
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir

@DisplayName("HTTP Fixture Recorder Tests")
class FixtureRecorderTest {

    @TempDir
    lateinit var temp: Path

    private lateinit var server: HttpServer
    private lateinit var base: String

    private val root: Path
        get() = temp.resolve("fixtures")
    private val staging: Path
        get() = temp.resolve("staging")

    @BeforeEach
    fun startServer() {
        server = HttpServer.create(InetSocketAddress(InetAddress.getLoopbackAddress(), 0), 0)
        server.createContext("/") { exchange ->
            val path = exchange.requestURI.path
            val (status, body) = when (path) {
                "/listing/" -> 200 to "a b"
                "/listing/a" -> 200 to "child a"
                "/listing/b" -> 200 to "child b"
                "/plain" -> 200 to "plain"
                else -> 404 to ""
            }
            val bytes = body.toByteArray()
            exchange.sendResponseHeaders(status, if (bytes.isEmpty()) -1 else bytes.size.toLong())
            exchange.responseBody.use { it.write(bytes) }
        }
        server.start()
        base = "http://127.0.0.1:${server.address.port}"
    }

    @AfterEach
    fun stopServer() {
        server.stop(0)
    }

    private val listing: Endpoint
        get() = Endpoint("$base/listing/", expand = { body ->
            body.decodeToString().split(' ').map { Endpoint("$base/listing/$it") }
        })

    private fun record(vararg endpoints: Endpoint, filters: List<String> = emptyList()): Boolean =
        FixtureRecorder(root, staging, endpoints.toList(), filters).run()

    private fun recorded(url: String): Boolean = HttpFixtures.mappingPath(root, url).exists()

    @Test
    @DisplayName("Filters can pick out endpoints that only appear in a listing")
    fun filterListedEndpoint() {
        assertTrue(record(listing, Endpoint("$base/plain"), filters = listOf("listing/b")))

        assertTrue(recorded("$base/listing/b"))
        assertFalse(recorded("$base/listing/"))
        assertFalse(recorded("$base/listing/a"))
        assertFalse(recorded("$base/plain"))
        assertFalse(staging.exists())
    }

    @Test
    @DisplayName("A filter that matches nothing fails")
    fun filterMatchesNothing() {
        assertFalse(record(listing, Endpoint("$base/plain"), filters = listOf("nothing")))
        assertFalse(root.exists() && root.toFile().walk().any { it.isFile })
    }

    @Test
    @DisplayName("A full recording replaces the root through the staging directory")
    fun fullRecording() {
        HttpFixtures.write(root, "$base/stale", 200, emptyMap(), "old".toByteArray())

        assertTrue(record(listing, Endpoint("$base/plain")))

        for (url in listOf("$base/listing/", "$base/listing/a", "$base/listing/b", "$base/plain")) {
            assertTrue(recorded(url), url)
        }
        assertFalse(recorded("$base/stale"))
        assertFalse(staging.exists())
        val body = HttpFixtures.bodyPath(root, "$base/plain").readText()
        assertEquals("plain", body)
    }

    @Test
    @DisplayName("A failed full recording leaves the root unchanged")
    fun failedFullRecording() {
        HttpFixtures.write(root, "$base/stale", 200, emptyMap(), "old".toByteArray())

        assertFalse(record(Endpoint("$base/plain"), Endpoint("$base/missing")))

        assertTrue(recorded("$base/stale"))
        assertFalse(recorded("$base/plain"))
        assertFalse(staging.exists())
    }
}
