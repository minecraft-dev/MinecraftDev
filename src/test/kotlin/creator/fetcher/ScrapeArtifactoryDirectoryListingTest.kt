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

import com.demonwav.mcdev.creator.scrapeArtifactoryDirectoryListing
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

@DisplayName("Artifactory Directory Listing Fetcher Tests")
class ScrapeArtifactoryDirectoryListingTest : FetcherTestBase() {

    private val url = "https://maven.parchmentmc.org/org/parchmentmc/data/"

    @Test
    @DisplayName("Entries are read from the recorded listing, without the parent directory")
    fun recorded() {
        val entries = fetch { scrapeArtifactoryDirectoryListing(url) }!!
        assertEquals(27, entries.size)
        assertEquals("blackstone/", entries.first())
        assertEquals("parchment-1.21.9/", entries.last())
        assertFalse("../" in entries)
        assertNothingLogged()
    }

    @Test
    @DisplayName("A listing with no entries gives an empty list")
    fun empty() {
        stubBody(url, "<html><body><pre><a href=\"../\">../</a></pre></body></html>")
        assertEquals(emptyList<String>(), fetch { scrapeArtifactoryDirectoryListing(url) })
        assertNothingLogged()
    }

    @Test
    @DisplayName("Malformed HTML fails quietly")
    fun malformed() {
        stubBody(url, "<html><body><pre><a href=\"x/\">x/</pre>")
        assertFailsQuietly { scrapeArtifactoryDirectoryListing(url) }
    }

    @Test
    @DisplayName("An HTTP error fails quietly")
    fun httpError() {
        stubServerError(url)
        assertFailsQuietly { scrapeArtifactoryDirectoryListing(url) }
    }

    @Test
    @DisplayName("A missing listing fails quietly")
    fun notFound() {
        stubNotFound(url)
        assertFailsQuietly { scrapeArtifactoryDirectoryListing(url) }
    }
}
