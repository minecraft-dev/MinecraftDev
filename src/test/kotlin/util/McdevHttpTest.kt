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

import com.intellij.testFramework.fixtures.IdeaTestFixtureFactory
import com.sun.net.httpserver.HttpServer
import java.net.InetAddress
import java.net.InetSocketAddress
import java.util.Collections
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

@DisplayName("McdevHttp Tests")
class McdevHttpTest {

    private val base = "http://127.0.0.1:54321"

    // The test task sets the property, so put it back for the tests after these
    private val originalProperty: String? = System.getProperty(McdevHttp.MOCK_PROPERTY)

    @AfterEach
    fun restoreProperty() {
        if (originalProperty == null) {
            System.clearProperty(McdevHttp.MOCK_PROPERTY)
        } else {
            System.setProperty(McdevHttp.MOCK_PROPERTY, originalProperty)
        }
    }

    @Test
    @DisplayName("The test task keeps requests off the real network")
    fun testTaskBlocksNetwork() {
        assertTrue(originalProperty?.startsWith("http://127.0.0.1:") == true) {
            "Expected the test task to set ${McdevHttp.MOCK_PROPERTY} to a loopback URL, was $originalProperty"
        }
    }

    @Test
    @DisplayName("URLs are unchanged when the property is unset")
    fun unsetLeavesUrlsAlone() {
        System.clearProperty(McdevHttp.MOCK_PROPERTY)
        assertNull(McdevHttp.mockBaseUrl)
        val url = "https://meta.fabricmc.net/v2/versions"
        assertEquals(url, McdevHttp.resolve(url))
    }

    @Test
    @DisplayName("Blank property counts as unset")
    fun blankPropertyIsUnset() {
        System.setProperty(McdevHttp.MOCK_PROPERTY, "  ")
        assertNull(McdevHttp.mockBaseUrl)
    }

    @Test
    @DisplayName("Host becomes the first path segment")
    fun rewritesHost() {
        assertEquals(
            "$base/meta.fabricmc.net/v2/versions",
            McdevHttp.rewrite("https://meta.fabricmc.net/v2/versions", base),
        )
        assertEquals("$base/example.com", McdevHttp.rewrite("https://example.com", base))
        assertEquals("$base/example.com/", McdevHttp.rewrite("https://example.com/", base))
    }

    @Test
    @DisplayName("Query, encoding and port are kept")
    fun keepsQueryEncodingAndPort() {
        assertEquals(
            "$base/registry.npmjs.org/@fabricmc%2Fcli/latest",
            McdevHttp.rewrite("https://registry.npmjs.org/@fabricmc%2Fcli/latest", base),
        )
        assertEquals(
            "$base/example.com:8443/a%20b?x=1&y=%2F",
            McdevHttp.rewrite("https://example.com:8443/a%20b?x=1&y=%2F#frag", base),
        )
    }

    @Test
    @DisplayName("Non-HTTP and malformed URLs are unchanged")
    fun leavesOtherUrlsAlone() {
        for (url in listOf("file:///tmp/foo.zip", "jar:file:///tmp/foo.zip!/", "not a url", "/relative/path")) {
            assertEquals(url, McdevHttp.rewrite(url, base))
        }
    }

    @Test
    @DisplayName("Trailing slash on the base URL is ignored")
    fun trailingSlash() {
        System.setProperty(McdevHttp.MOCK_PROPERTY, "$base/")
        val expected = "$base/example.com/x"
        assertEquals(expected, McdevHttp.resolve("https://example.com/x"))
    }

    @Test
    @DisplayName("Requests reach a local server through the rewrite")
    fun requestsReachLocalServer() {
        // The default user agent reads the plugin version, which needs the IDE application
        val fixture = IdeaTestFixtureFactory.getFixtureFactory().createBareFixture()
        fixture.setUp()
        val seen = Collections.synchronizedList(mutableListOf<Map<String, String?>>())
        val server = HttpServer.create(InetSocketAddress(InetAddress.getLoopbackAddress(), 0), 0)
        server.createContext("/") { exchange ->
            seen += mapOf(
                "uri" to exchange.requestURI.toString(),
                "user-agent" to exchange.requestHeaders.getFirst("User-Agent"),
                "authorization" to exchange.requestHeaders.getFirst("Authorization"),
            )
            val body = "ok".toByteArray()
            exchange.sendResponseHeaders(200, body.size.toLong())
            exchange.responseBody.use { it.write(body) }
        }
        server.start()
        try {
            System.setProperty(McdevHttp.MOCK_PROPERTY, "http://127.0.0.1:${server.address.port}")

            val blocking = McdevHttp.request("https://registry.invalid/@scope%2Fname/latest").readString()
            val suspending = runBlocking {
                McdevHttp.get("https://maven.invalid/a/maven-metadata.xml?x=1", mapOf("Authorization" to "Basic abc")).suspend().connect {
                    readString()
                }
            }

            assertEquals(listOf("ok", "ok"), listOf(blocking, suspending))
            assertEquals(
                listOf("/registry.invalid/@scope%2Fname/latest", "/maven.invalid/a/maven-metadata.xml?x=1"),
                seen.map { it["uri"] },
            )
            assertEquals(listOf(null, "Basic abc"), seen.map { it["authorization"] })
            assertTrue(seen.all { it["user-agent"]?.startsWith("github_org/minecraft-dev/") == true }) {
                "Unexpected user agents: ${seen.map { it["user-agent"] }}"
            }
        } finally {
            server.stop(0)
            fixture.tearDown()
        }
    }

    @Test
    @DisplayName("Extra headers are only sent to the original host")
    fun headersStayOnOriginalHost() {
        val fixture = IdeaTestFixtureFactory.getFixtureFactory().createBareFixture()
        fixture.setUp()

        val auth = Collections.synchronizedMap(mutableMapOf<String, String?>())
        fun HttpServer.respond(path: String, status: Int, location: String? = null) {
            createContext(path) { exchange ->
                auth[exchange.requestURI.path] = exchange.requestHeaders.getFirst("Authorization")
                location?.let { exchange.responseHeaders.add("Location", it) }
                val body = if (status == 200) "ok".toByteArray() else ByteArray(0)
                exchange.sendResponseHeaders(status, if (body.isEmpty()) -1 else body.size.toLong())
                exchange.responseBody.use { it.write(body) }
            }
        }

        // Two hosts that are both this machine, under different names
        val original = HttpServer.create(InetSocketAddress(InetAddress.getByName("127.0.0.1"), 0), 0)
        val other = HttpServer.create(InetSocketAddress(InetAddress.getByName("localhost"), 0), 0)
        val otherBase = "http://localhost:${other.address.port}"
        original.respond("/example.invalid/same", 302, "/example.invalid/final")
        original.respond("/example.invalid/final", 200)
        original.respond("/example.invalid/away", 302, "$otherBase/elsewhere")
        other.respond("/elsewhere", 200)
        original.start()
        other.start()
        try {
            System.setProperty(McdevHttp.MOCK_PROPERTY, "http://127.0.0.1:${original.address.port}")
            val headers = mapOf("Authorization" to "Basic abc")

            assertEquals("ok", McdevHttp.request("https://example.invalid/same", headers).readString())
            assertEquals("ok", McdevHttp.request("https://example.invalid/away", headers).readString())

            assertEquals(
                mapOf(
                    "/example.invalid/same" to "Basic abc",
                    "/example.invalid/final" to "Basic abc",
                    "/example.invalid/away" to "Basic abc",
                    "/elsewhere" to null,
                ),
                auth.toMap(),
            )
        } finally {
            original.stop(0)
            other.stop(0)
            fixture.tearDown()
        }
    }
}
