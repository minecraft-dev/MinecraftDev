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

package com.demonwav.mcdev.creator.fetcher

import com.demonwav.mcdev.creator.collectMavenVersions
import com.github.tomakehurst.wiremock.client.WireMock.aResponse
import com.github.tomakehurst.wiremock.client.WireMock.ok
import com.github.tomakehurst.wiremock.http.Fault
import kotlin.time.Duration.Companion.milliseconds
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.withTimeout
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

@DisplayName("Maven Metadata Fetcher Tests")
class CollectMavenVersionsTest : FetcherTestBase() {

    private val url = "https://maven.fabricmc.net/net/fabricmc/fabric-loom/maven-metadata.xml"

    @Test
    @DisplayName("Versions are read from recorded metadata in file order")
    fun recorded() {
        val versions = fetch { collectMavenVersions(url) }.getOrThrow()
        assertEquals(101, versions.size)
        assertEquals("1.20.0-alpha.31", versions.last())
        assertTrue("1.19.4" in versions)
        assertNothingLogged()
    }

    @Test
    @DisplayName("The filter drops versions")
    fun filter() {
        val versions = fetch { collectMavenVersions(url) { !it.contains('-') } }.getOrThrow()
        assertTrue(versions.isNotEmpty())
        assertTrue(versions.none { it.contains('-') }) { versions.toString() }
    }

    @Test
    @DisplayName("Headers are sent with the request")
    fun headers() {
        fetch { collectMavenVersions(url, mapOf("Authorization" to "Bearer token")) }
        assertEquals("Bearer token", http.server.requestsTo(url).single().getHeader("Authorization"))
    }

    @Test
    @DisplayName("Whitespace around versions is trimmed and empty versions are skipped")
    fun whitespace() {
        stubBody(url, mavenMetadata(" 1.0 ", "", "\n2.0\n"))
        assertEquals(listOf("1.0", "2.0"), fetch { collectMavenVersions(url) }.getOrThrow())
        assertNothingLogged()
    }

    @Test
    @DisplayName("Metadata with no versions gives an empty list")
    fun empty() {
        stubBody(url, "<metadata><versioning><versions></versions></versioning></metadata>")
        assertEquals(emptyList<String>(), fetch { collectMavenVersions(url) }.getOrThrow())
        assertNothingLogged()
    }

    @Test
    @DisplayName("An empty body fails quietly")
    fun emptyBody() {
        stubBody(url, "")
        assertFailsQuietly { collectMavenVersions(url) }
    }

    @Test
    @DisplayName("Malformed XML fails quietly")
    fun malformed() {
        stubBody(url, MALFORMED_XML)
        assertFailsQuietly { collectMavenVersions(url) }
    }

    @Test
    @DisplayName("An HTML error page fails quietly")
    fun htmlPage() {
        stubBody(url, "<html><body><p>Bad gateway<br></p></body></html>")
        assertFailsQuietly { collectMavenVersions(url) }
    }

    @Test
    @DisplayName("An HTTP error fails quietly")
    fun httpError() {
        stubServerError(url)
        assertFailsQuietly { collectMavenVersions(url) }
    }

    @Test
    @DisplayName("A dropped connection fails quietly")
    fun droppedConnection() {
        stub(url, aResponse().withFault(Fault.CONNECTION_RESET_BY_PEER))
        assertFailsQuietly { collectMavenVersions(url) }
    }

    @Test
    @DisplayName("Cancellation propagates instead of being logged")
    fun cancellation() {
        stub(url, ok(mavenMetadata("1.0")).withFixedDelay(50))
        assertThrows<TimeoutCancellationException> {
            fetch { withTimeout(5.milliseconds) { collectMavenVersions(url) } }
        }
        assertNothingLogged()
    }
}
