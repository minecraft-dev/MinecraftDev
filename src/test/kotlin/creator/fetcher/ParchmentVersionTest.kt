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

import com.demonwav.mcdev.creator.ParchmentVersion
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

@DisplayName("Parchment Version Fetcher Tests")
class ParchmentVersionTest : FetcherTestBase() {

    private val baseUrl = "https://maven.parchmentmc.org/org/parchmentmc/data/"

    private fun metadataUrl(mcVersion: String) = "${baseUrl}parchment-$mcVersion/maven-metadata.xml"

    private fun listing(vararg entries: String): String {
        val links = entries.joinToString("\n") { "<a href=\"$it\">$it</a>" }
        return "<html><body><pre><a href=\"../\">../</a>\n$links</pre></body></html>"
    }

    @Test
    @DisplayName("Versions are read from recorded data, newest Minecraft version first")
    fun recorded() {
        val versions = fetch { ParchmentVersion.downloadData() }!!
        assertEquals(50, versions.size)
        assertEquals(
            listOf("1.21.11 - BLEEDING-SNAPSHOT", "1.21.11 - 2025.12.20", "1.21.11 - 2025.12.21-nightly-SNAPSHOT"),
            versions.take(3).map { it.toString() },
        )
        assertEquals("1.21.10 - BLEEDING-SNAPSHOT", versions[3].toString())
        assertNothingLogged()
    }

    @Test
    @DisplayName("Downloading stops once the limit is reached")
    fun limit() {
        val versions = fetch { ParchmentVersion.downloadData(limit = 4) }!!
        assertEquals(4, versions.size)
        assertEquals(1, http.server.requestsTo(metadataUrl("1.21.10")).size)
        assertEquals(0, http.server.requestsTo(metadataUrl("1.21.9")).size)
    }

    @Test
    @DisplayName("Minecraft versions without a release are skipped silently")
    fun notFound() {
        stubNotFound(metadataUrl("1.21.11"))
        val versions = fetch { ParchmentVersion.downloadData() }!!
        assertEquals("1.21.10 - BLEEDING-SNAPSHOT", versions.first().toString())
        assertNothingLogged()
    }

    @Test
    @DisplayName("A Minecraft version that fails to download is skipped with a warning")
    fun partialFailure() {
        stubServerError(metadataUrl("1.21.11"))
        val versions = fetch { ParchmentVersion.downloadData() }!!
        assertEquals("1.21.10 - BLEEDING-SNAPSHOT", versions.first().toString())
        assertTrue(log.warnings.isNotEmpty())
    }

    @Test
    @DisplayName("Malformed metadata for a Minecraft version is skipped with a warning")
    fun partialMalformed() {
        stubBody(metadataUrl("1.21.11"), MALFORMED_XML)
        val versions = fetch { ParchmentVersion.downloadData() }!!
        assertEquals("1.21.10 - BLEEDING-SNAPSHOT", versions.first().toString())
        assertTrue(log.warnings.isNotEmpty())
    }

    @Test
    @DisplayName("Entries that aren't Parchment versions are ignored")
    fun otherEntries() {
        stubBody(baseUrl, listing("blackstone/", "parchment-1.21.11/", "parchment-not.a.version/"))
        val versions = fetch { ParchmentVersion.downloadData() }!!
        assertEquals(
            listOf("1.21.11 - BLEEDING-SNAPSHOT", "1.21.11 - 2025.12.20", "1.21.11 - 2025.12.21-nightly-SNAPSHOT"),
            versions.map { it.toString() },
        )
        assertNothingLogged()
    }

    @Test
    @DisplayName("No Minecraft version with a release fails quietly")
    fun noReleases() {
        stubBody(baseUrl, listing("parchment-1.21.11/"))
        stubNotFound(metadataUrl("1.21.11"))
        assertFailsQuietly { ParchmentVersion.downloadData() }
    }

    @Test
    @DisplayName("Every Minecraft version failing to download fails quietly")
    fun allFail() {
        stubBody(baseUrl, listing("parchment-1.21.10/", "parchment-1.21.11/"))
        stubServerError(metadataUrl("1.21.10"))
        stubServerError(metadataUrl("1.21.11"))
        assertFailsQuietly { ParchmentVersion.downloadData() }
    }

    @Test
    @DisplayName("An empty listing fails quietly")
    fun emptyListing() {
        stubBody(baseUrl, listing())
        assertFailsQuietly { ParchmentVersion.downloadData() }
    }

    @Test
    @DisplayName("A listing that fails to download fails quietly")
    fun listingFailure() {
        stubServerError(baseUrl)
        assertFailsQuietly { ParchmentVersion.downloadData() }
    }
}
