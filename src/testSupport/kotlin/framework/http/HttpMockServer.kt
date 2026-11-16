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

import com.github.tomakehurst.wiremock.WireMockServer
import com.github.tomakehurst.wiremock.client.MappingBuilder
import com.github.tomakehurst.wiremock.client.ResponseDefinitionBuilder
import com.github.tomakehurst.wiremock.client.WireMock
import com.github.tomakehurst.wiremock.core.WireMockConfiguration
import com.github.tomakehurst.wiremock.extension.ResponseTransformerV2
import com.github.tomakehurst.wiremock.http.Response
import com.github.tomakehurst.wiremock.http.ResponseDefinition
import com.github.tomakehurst.wiremock.matching.UrlPattern
import com.github.tomakehurst.wiremock.standalone.MappingsSource
import com.github.tomakehurst.wiremock.stubbing.ServeEvent
import com.github.tomakehurst.wiremock.stubbing.StubMapping
import com.github.tomakehurst.wiremock.stubbing.StubMappings
import com.github.tomakehurst.wiremock.verification.LoggedRequest
import java.net.URI
import java.net.URISyntaxException
import java.nio.file.Files
import java.nio.file.Path
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.CopyOnWriteArrayList
import kotlin.io.path.extension
import kotlin.io.path.isDirectory
import kotlin.io.path.isRegularFile
import kotlin.io.path.readBytes
import kotlin.io.path.readText
import kotlin.time.Duration

/**
 * A WireMock server on a free loopback port, loaded with the stub mappings from [fixtureRoots]. Tests can override
 * fixtures by adding a stub for the same URL.
 *
 * Point the plugin at it with [MOCK_PROPERTY] set to [baseUrl], or use [HttpMockExtension], which does that for each
 * test. Match requests with [mockUrl], since the server sees `https://host/path` as `/host/path`.
 *
 * The server refuses to send a redirect to an absolute URL, because the client would follow it to the real host. It
 * answers 500 instead, and [verify] reports it.
 */
class HttpMockServer(fixtureRoots: List<Path> = HttpFixtures.defaultRoots()) : AutoCloseable {
    private val fixtures = FixtureMappings(fixtureRoots)
    private val redirectProblems = CopyOnWriteArrayList<String>()

    val wireMock: WireMockServer = WireMockServer(
        WireMockConfiguration.options()
            .bindAddress(LOOPBACK)
            .dynamicPort()
            .mappingSource(fixtures)
            .extensions(RefuseAbsoluteRedirects(redirectProblems)),
    ).apply { start() }

    val port: Int
        get() = wireMock.port()

    @Suppress("HttpUrlsUsage")
    val baseUrl: String
        get() = "http://$LOOPBACK:$port"

    /**
     * When true, [verify] fails on requests no stub matched. They get a 404 either way.
     */
    @Volatile
    var strict: Boolean = true

    /**
     * Requests received so far, oldest first.
     */
    val requests: List<LoggedRequest>
        get() = wireMock.allServeEvents.asReversed().map { it.request }

    /**
     * The original URL of each request received so far, oldest first.
     */
    val requestedUrls: List<String>
        get() = requests.map { HttpFixturePaths.originalUrl(it.url) ?: it.url }

    fun requestsTo(url: String): List<LoggedRequest> {
        val path = mockPath(url)
        return requests.filter { it.url == path }
    }

    /**
     * Everything [verify] would fail on.
     */
    val problems: List<String>
        get() = buildList {
            addAll(redirectProblems)
            if (strict) {
                for (request in wireMock.findAllUnmatchedRequests()) {
                    val url = HttpFixturePaths.originalUrl(request.url) ?: request.url
                    val expected = try {
                        HttpFixtures.mappingFileName(url)
                    } catch (_: IllegalArgumentException) {
                        "a mapping matching ${request.url}"
                    }
                    add("No fixture for ${request.method} $url (expected at $expected)")
                }
            }
        }

    fun stubFor(mapping: MappingBuilder): StubMapping = wireMock.stubFor(mapping)

    /**
     * A copy of the recorded response for [url], to stub with changes such as a delay. `withBodyFile` can't find
     * fixture bodies, so use this to reuse one. Throws [IllegalStateException] if [url] has no fixture.
     */
    fun fixture(url: String): ResponseDefinitionBuilder {
        val response = checkNotNull(fixtures.responses[mockPath(url)]) { "No fixture for $url" }
        return ResponseDefinitionBuilder.like(response)
    }

    /**
     * Makes the recorded response for [url] arrive after [delay]. Overrides any earlier stub for the same URL.
     */
    fun delay(url: String, delay: Duration) {
        stubFor(WireMock.get(mockUrl(url)).willReturn(fixture(url).withFixedDelay(delay.inWholeMilliseconds.toInt())))
    }

    /**
     * Throws [AssertionError] listing [problems], if there are any.
     */
    fun verify() {
        val problems = problems
        if (problems.isNotEmpty()) {
            throw AssertionError("The HTTP mock server saw problems:\n" + problems.joinToString("\n") { "  $it" })
        }
    }

    override fun close() {
        wireMock.stop()
    }

    companion object {
        /**
         * The system property `McdevHttp` reads the mock server's base URL from.
         */
        const val MOCK_PROPERTY = "mcdev.test.httpMock"

        private const val LOOPBACK = "127.0.0.1"

        /**
         * The path and query the server receives for [url].
         */
        fun mockPath(url: String): String = HttpFixturePaths.mockPath(url)

        /**
         * Matches requests for [url] exactly, including the query string.
         */
        fun mockUrl(url: String): UrlPattern = WireMock.urlEqualTo(mockPath(url))
    }
}

/**
 * Loads the stub mappings from each fixture root, with bodies read from that root's `__files` directory. WireMock only
 * resolves `bodyFileName` against one directory, so the bodies are read here and inlined.
 */
private class FixtureMappings(private val roots: List<Path>) : MappingsSource {
    /**
     * Fixture responses by exact request URL.
     */
    val responses = ConcurrentHashMap<String, ResponseDefinition>()

    override fun loadMappingsInto(stubMappings: StubMappings) {
        responses.clear()
        // On equal priority WireMock prefers the stub added last, so add the first root last
        for (root in roots.asReversed()) {
            for (mapping in load(root)) {
                stubMappings.addMapping(mapping)
                mapping.request.url?.let { responses[it] = mapping.response }
            }
        }
    }

    private fun load(root: Path): List<StubMapping> {
        val mappings = root.resolve(HttpFixtures.MAPPINGS_DIR)
        if (!mappings.isDirectory()) {
            return emptyList()
        }
        val files = Files.walk(mappings).use { paths ->
            paths.filter { it.isRegularFile() && it.extension == "json" }.sorted().toList()
        }
        return files.map { file ->
            val mapping = try {
                StubMapping.buildFrom(file.readText())
            } catch (e: Exception) {
                throw IllegalStateException("Couldn't read the fixture mapping $file", e)
            }
            val bodyFileName = mapping.response.bodyFileName
            if (bodyFileName != null) {
                val body = root.resolve(HttpFixtures.FILES_DIR).resolve(bodyFileName)
                check(body.isRegularFile()) { "The fixture mapping $file points at $body, which doesn't exist" }
                mapping.response = ResponseDefinitionBuilder.like(mapping.response)
                    .withBodyFile(null)
                    .withBody(body.readBytes())
                    .build()
            }
            mapping
        }
    }

    // Fixtures are read only, WireMock calls these when stubs are saved or removed through its API
    override fun save(mappings: List<StubMapping>) {}

    override fun save(mapping: StubMapping) {}

    @Suppress("removal")
    @Deprecated("Deprecated in Java")
    override fun remove(mapping: StubMapping) {}

    override fun removeAll() {}
}

private class RefuseAbsoluteRedirects(private val problems: MutableList<String>) : ResponseTransformerV2 {
    override fun getName(): String = "mcdev-refuse-absolute-redirects"

    override fun transform(response: Response, serveEvent: ServeEvent): Response {
        val location = response.headers.getHeader("Location")
        if (!location.isPresent || !isAbsolute(location.firstValue())) {
            return response
        }
        val message = "Refused to redirect ${serveEvent.request.url} to ${location.firstValue()}. " +
            "The client would leave the mock server, use a path on it instead."
        problems += message
        return Response.response().status(500).body(message).build()
    }

    private fun isAbsolute(location: String): Boolean = location.startsWith("//") ||
        try {
            URI(location).isAbsolute
        } catch (_: URISyntaxException) {
            false
        }
}
