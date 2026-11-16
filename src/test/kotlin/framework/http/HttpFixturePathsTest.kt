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

import com.demonwav.mcdev.util.McdevHttp
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

@DisplayName("HTTP Fixture Path Tests")
class HttpFixturePathsTest {

    private fun path(url: String) = HttpFixturePaths.pathFor(url)

    @Test
    @DisplayName("Host and path segments become directories")
    fun plainUrl() {
        assertEquals("meta.fabricmc.net/v2/versions", path("https://meta.fabricmc.net/v2/versions"))
        assertEquals(
            "maven.neoforged.net/releases/net/neoforged/neoforge/maven-metadata.xml",
            path("https://maven.neoforged.net/releases/net/neoforged/neoforge/maven-metadata.xml"),
        )
    }

    @Test
    @DisplayName("Scheme and host case don't matter, the fragment is ignored")
    fun normalization() {
        val expected = "example.com/A/b"
        assertEquals(expected, path("https://example.com/A/b"))
        assertEquals(expected, path("http://EXAMPLE.com/A/b"))
        assertEquals(expected, path("https://example.com/A/b#section"))
    }

    @Test
    @DisplayName("Directory URLs are stored as an index file")
    fun directoryUrls() {
        assertEquals("example.com/a/_index", path("https://example.com/a/"))
        assertEquals("example.com/_index", path("https://example.com/"))
        assertEquals("example.com/_index", path("https://example.com"))
    }

    @Test
    @DisplayName("Query strings and ports are encoded into names")
    fun queryAndPort() {
        assertEquals("example.com/a/b%3Fx=1&y=a%3Ab%2Fc", path("https://example.com/a/b?x=1&y=a:b/c"))
        assertEquals("example.com%3A8443/a", path("https://example.com:8443/a"))
        assertEquals("example.com/_index%3Fq=1", path("https://example.com/?q=1"))
    }

    @Test
    @DisplayName("Raw percent-encoding in the path is kept")
    fun rawEncoding() {
        val npm = "https://registry.npmjs.org/@fabricmc%2Fcli/latest"
        assertEquals("registry.npmjs.org/@fabricmc%2Fcli/latest", path(npm))
        assertEquals("example.com/a%20b", path("https://example.com/a%20b"))
    }

    @Test
    @DisplayName("Names that aren't valid on every OS are escaped")
    fun unsafeNames() {
        assertEquals("example.com/%63on/%61ux.txt/%4EUL", path("https://example.com/con/aux.txt/NUL"))
        assertEquals("example.com/%63on.tar.gz", path("https://example.com/con.tar.gz"))
        // Only the part before the first dot can be reserved
        val notReserved = "example.com/icons.json/a.con.txt/console"
        assertEquals(notReserved, path("https://$notReserved"))
        assertEquals("example.com/a/name%2E", path("https://example.com/a/name."))
        assertEquals("example.com/%25zz/%E2%9C%93", path("https://example.com/%25zz/%E2%9C%93"))
        for (segment in HttpFixturePaths.segmentsFor("https://example.com/a*b/c:d?e=f/g?h")) {
            assertTrue(segment.none { it in ":?*<>|\"\\" }) { "Unsafe file name $segment" }
        }
    }

    @Test
    @DisplayName("Long names are shortened with a hash")
    fun longNames() {
        val first = path("https://example.com/search?q=" + "a".repeat(300))
        val second = path("https://example.com/search?q=" + "a".repeat(299) + "b")
        val name = first.substringAfterLast('/')
        assertEquals(HttpFixturePaths.MAX_NAME_LENGTH, name.length)
        assertTrue(name.startsWith("search%3Fq=aaa"))
        assertNotEquals(first, second)
        assertEquals(first, path("https://example.com/search?q=" + "a".repeat(300)))
    }

    @Test
    @DisplayName("Only absolute HTTP URLs are accepted")
    fun invalidUrls() {
        for (url in listOf("file:///tmp/a.zip", "/relative", "not a url", "https:///no-host")) {
            assertThrows<IllegalArgumentException>(url) { path(url) }
        }
    }

    @Test
    @DisplayName("Mock paths match the routing helper's rewrite")
    fun mockPath() {
        val urls = listOf(
            "https://meta.fabricmc.net/v2/versions",
            "https://registry.npmjs.org/@fabricmc%2Fcli/latest",
            "https://example.com:8443/a%20b/?x=1&y=%2F",
            "https://example.com",
            "https://example.com/a#fragment",
        )
        for (url in urls) {
            assertEquals(McdevHttp.rewrite(url, ""), HttpFixturePaths.mockPath(url), url)
        }
    }

    @Test
    @DisplayName("Mock paths map back to the original URL")
    fun originalUrl() {
        for (url in listOf("https://meta.fabricmc.net/v2/versions", "https://example.com:8443/a%20b/?x=1")) {
            assertEquals(url, HttpFixturePaths.originalUrl(HttpFixturePaths.mockPath(url)))
        }
        assertEquals("https://example.com", HttpFixturePaths.originalUrl("/example.com"))
        assertNull(HttpFixturePaths.originalUrl("/"))
    }
}
