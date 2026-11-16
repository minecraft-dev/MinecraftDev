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

import com.demonwav.mcdev.creator.custom.TemplateDescriptor
import com.demonwav.mcdev.framework.http.HttpMockServer.Companion.mockUrl
import com.demonwav.mcdev.util.McdevHttp
import com.github.tomakehurst.wiremock.client.WireMock.aResponse
import com.github.tomakehurst.wiremock.client.WireMock.get
import com.github.tomakehurst.wiremock.client.WireMock.ok
import com.github.tomakehurst.wiremock.client.WireMock.okJson
import com.github.tomakehurst.wiremock.client.WireMock.serverError
import com.github.tomakehurst.wiremock.client.WireMock.temporaryRedirect
import com.github.tomakehurst.wiremock.client.WireMock.urlPathMatching
import com.github.tomakehurst.wiremock.http.Fault
import com.intellij.testFramework.fixtures.IdeaTestFixture
import com.intellij.testFramework.fixtures.IdeaTestFixtureFactory
import com.intellij.util.io.HttpRequests
import java.io.ByteArrayInputStream
import java.io.IOException
import java.nio.file.Path
import java.util.zip.ZipInputStream
import kotlin.io.path.createDirectories
import kotlin.io.path.writeText
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.TimeSource
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.api.extension.RegisterExtension
import org.junit.jupiter.api.io.TempDir

@DisplayName("HTTP Mock Server Tests")
class HttpMockServerTest {

    @JvmField
    @RegisterExtension
    val http = HttpMockExtension()

    private val paper = "https://fill.papermc.io/v3/projects/paper"
    private val fabric = "https://meta.fabricmc.net/v2/versions"

    // The plugin's user agent reads the plugin version, which needs the IDE application
    private lateinit var fixture: IdeaTestFixture

    @BeforeEach
    fun setUp() {
        fixture = IdeaTestFixtureFactory.getFixtureFactory().createBareFixture()
        fixture.setUp()
    }

    @AfterEach
    fun tearDown() {
        fixture.tearDown()
    }

    private fun get(url: String): String = McdevHttp.request(url).readString()

    /**
     * Runs [block] against a separate server, so the problems it's expected to record don't fail the test.
     */
    private fun <T> withServer(server: HttpMockServer, block: () -> T): T {
        System.setProperty(HttpMockServer.MOCK_PROPERTY, server.baseUrl)
        try {
            return block()
        } finally {
            System.setProperty(HttpMockServer.MOCK_PROPERTY, http.server.baseUrl)
        }
    }

    @Test
    @DisplayName("The server and the plugin read the same property")
    fun propertyName() {
        assertEquals(McdevHttp.MOCK_PROPERTY, HttpMockServer.MOCK_PROPERTY)
        assertEquals(http.server.baseUrl, McdevHttp.mockBaseUrl)
    }

    @Test
    @DisplayName("Recorded fixtures are served through the routing helper")
    fun servesFixtures() {
        assertTrue("\"versions\"" in get(paper))
        val suspending = runBlocking { McdevHttp.getString(fabric) }
        assertTrue("\"game\"" in suspending)

        assertEquals(listOf(paper, fabric), http.server.requestedUrls)
        val agents = http.server.requests.map { it.getHeader("User-Agent") }
        assertTrue(agents.all { it?.contains("/minecraft-dev/") == true }) { agents.toString() }
    }

    @Test
    @DisplayName("The builtin template download is the pinned templates checkout")
    fun servesBuiltinTemplates() {
        val version = TemplateDescriptor.LATEST_FORMAT_VERSION
        val url = "https://github.com/minecraft-dev/templates/archive/refs/heads/v$version.zip"
        val bytes = McdevHttp.request(url).readBytes(null)
        val names = ZipInputStream(ByteArrayInputStream(bytes)).use { zip ->
            generateSequence { zip.nextEntry?.name }.toList()
        }
        assertTrue(names.all { it.startsWith("templates-$version/") }) { names.take(5).toString() }
        assertTrue("templates-$version/fabric/.mcdev.template.json" in names)
        assertTrue(names.none { it.endsWith("/.git") })
    }

    @Test
    @DisplayName("Unknown URLs answer 404 and are recorded as a problem")
    fun strictMode() {
        HttpMockServer(emptyList()).use { server ->
            val error = withServer(server) {
                assertThrows<HttpRequests.HttpStatusException> { get("https://example.invalid/nothing?x=1") }
            }
            assertEquals(404, error.statusCode)
            val expected = "No fixture for GET https://example.invalid/nothing?x=1 " +
                "(expected at mappings/example.invalid/nothing%3Fx=1.json)"
            assertEquals(listOf(expected), server.problems)
            val failure = assertThrows<AssertionError> { server.verify() }
            assertTrue("https://example.invalid/nothing?x=1" in failure.message.orEmpty())
        }
    }

    @Test
    @DisplayName("Lenient mode answers 404 without a problem")
    fun lenientMode() {
        http.server.strict = false
        val error = assertThrows<HttpRequests.HttpStatusException> { get("https://example.invalid/nothing") }
        assertEquals(404, error.statusCode)
        assertEquals(listOf("https://example.invalid/nothing"), http.server.requestedUrls)
        assertTrue(http.server.problems.isEmpty())
    }

    @Test
    @DisplayName("Stubs added in a test override fixtures, and the latest one wins")
    fun overrides() {
        http.server.stubFor(get(mockUrl(paper)).willReturn(serverError()))
        assertEquals(500, assertThrows<HttpRequests.HttpStatusException> { get(paper) }.statusCode)

        http.server.stubFor(get(mockUrl(paper)).willReturn(okJson("""{"versions":{}}""")))
        assertEquals("""{"versions":{}}""", get(paper))

        http.server.stubFor(get(urlPathMatching("/api\\.modrinth\\.com/.*")).willReturn(ok("malformed")))
        assertEquals("malformed", get("https://api.modrinth.com/v2/project/anything/version"))

        assertEquals(2, http.server.requestsTo(paper).size)
    }

    @Test
    @DisplayName("Stubs don't need a fixture, and can drop the connection")
    fun disconnect() {
        val url = "https://example.invalid/drop"
        http.server.stubFor(get(mockUrl(url)).willReturn(aResponse().withFault(Fault.EMPTY_RESPONSE)))
        val error = assertThrows<IOException> { get(url) }
        assertTrue(error !is HttpRequests.HttpStatusException) { error.toString() }
    }

    @Test
    @DisplayName("Delays only slow down the endpoint they're set on")
    fun delays() {
        http.server.delay(paper, 500.milliseconds)

        val fast = TimeSource.Monotonic.markNow()
        get(fabric)
        assertTrue(fast.elapsedNow() < 500.milliseconds) { "Undelayed request took ${fast.elapsedNow()}" }

        val slow = TimeSource.Monotonic.markNow()
        assertTrue("\"versions\"" in get(paper))
        assertTrue(slow.elapsedNow() >= 500.milliseconds) { "Delayed request took ${slow.elapsedNow()}" }
    }

    @Test
    @DisplayName("Redirects to absolute URLs are refused")
    fun absoluteRedirects() {
        HttpMockServer(emptyList()).use { server ->
            val url = "https://example.invalid/moved"
            server.stubFor(get(mockUrl(url)).willReturn(temporaryRedirect("https://codeload.github.com/real")))
            val error = withServer(server) {
                assertThrows<HttpRequests.HttpStatusException> { get(url) }
            }
            assertEquals(500, error.statusCode)
            assertTrue(server.problems.single().contains("https://codeload.github.com/real")) {
                server.problems.toString()
            }
        }
    }

    @Test
    @DisplayName("Relative redirects stay on the mock server")
    fun relativeRedirects() {
        val url = "https://example.invalid/moved"
        http.server.stubFor(get(mockUrl(url)).willReturn(temporaryRedirect(HttpMockServer.mockPath(paper))))
        assertTrue("\"versions\"" in get(url))
        assertEquals(listOf(url, paper), http.server.requestedUrls)
    }

    @Test
    @DisplayName("Hand-written mappings work, and the first root with a match wins")
    fun roots(@TempDir first: Path, @TempDir second: Path) {
        HttpFixtures.write(second, "https://example.com/a", 200, emptyMap(), "second".toByteArray())
        HttpFixtures.write(second, "https://example.com/b", 200, emptyMap(), "only second".toByteArray())
        HttpFixtures.write(first, "https://example.com/a", 200, emptyMap(), "first".toByteArray())
        first.resolve("mappings/handwritten.json").apply {
            parent.createDirectories()
            writeText(
                """
                {
                  "request": { "method": "GET", "urlPathPattern": "/example\\.com/search/.*" },
                  "response": { "status": 503, "body": "down", "headers": { "Retry-After": "5" } }
                }
                """.trimIndent(),
            )
        }

        HttpMockServer(listOf(first, second)).use { server ->
            withServer(server) {
                assertEquals("first", get("https://example.com/a"))
                assertEquals("only second", get("https://example.com/b"))
                val error = assertThrows<HttpRequests.HttpStatusException> { get("https://example.com/search/x") }
                assertEquals(503, error.statusCode)
            }
            assertEquals("first", server.fixture("https://example.com/a").build().byteBody.decodeToString())
            server.verify()
        }
    }
}
