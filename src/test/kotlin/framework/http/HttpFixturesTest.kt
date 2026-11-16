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

import com.demonwav.mcdev.framework.http.recorder.FixtureEndpoints
import java.nio.file.Path
import java.nio.file.Paths
import kotlin.io.path.exists
import kotlin.io.path.readBytes
import kotlin.io.path.readText
import org.junit.jupiter.api.Assertions.assertArrayEquals
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.api.io.TempDir

@DisplayName("HTTP Fixture Tests")
class HttpFixturesTest {

    @TempDir
    lateinit var root: Path

    @Test
    @DisplayName("A fixture is a WireMock mapping plus a body file, named after the URL")
    fun layout() {
        val url = "https://example.com/v2/project/a/version?x=1"
        HttpFixtures.write(root, url, 200, mapOf("Content-Type" to "application/json"), "[]".toByteArray())

        val expected = """
            {
              "name": "https://example.com/v2/project/a/version?x=1",
              "request": {
                "method": "GET",
                "url": "/example.com/v2/project/a/version?x=1"
              },
              "response": {
                "status": 200,
                "bodyFileName": "example.com/v2/project/a/version%3Fx=1",
                "headers": {
                  "Content-Type": "application/json"
                }
              }
            }
        """.trimIndent() + "\n"
        val name = "example.com/v2/project/a/version%3Fx=1"
        assertEquals(expected, root.resolve("mappings/$name.json").readText())
        assertArrayEquals("[]".toByteArray(), root.resolve("__files/$name").readBytes())
    }

    @Test
    @DisplayName("Writing the same response twice gives the same files")
    fun stable() {
        val url = "https://example.com/a"
        val headers = mapOf("X-B" to "2", "Content-Type" to "text/plain", "X-A" to "1")
        HttpFixtures.write(root, url, 200, headers, "x".toByteArray())
        val first = HttpFixtures.mappingPath(root, url).readText()
        HttpFixtures.write(root, url, 200, headers.entries.reversed().associate { it.toPair() }, "x".toByteArray())
        assertEquals(first, HttpFixtures.mappingPath(root, url).readText())
        assertTrue(first.indexOf("Content-Type") < first.indexOf("X-A")) { first }
    }

    @Test
    @DisplayName("A response without a body has no body file")
    fun noBody() {
        val url = "https://example.com/missing/maven-metadata.xml"
        HttpFixtures.write(root, url, 200, emptyMap(), "<metadata/>".toByteArray())
        HttpFixtures.write(root, url, 404, emptyMap(), null)

        val mapping = HttpFixtures.mappingPath(root, url).readText()
        assertTrue("\"status\": 404" in mapping) { mapping }
        assertFalse("bodyFileName" in mapping) { mapping }
        assertFalse(HttpFixtures.bodyPath(root, url).exists())
    }

    @Test
    @DisplayName("A path needed as both a file and a directory fails clearly")
    fun fileDirectoryClash() {
        HttpFixtures.write(root, "https://example.com/a", 200, emptyMap(), "x".toByteArray())
        val error = assertThrows<IllegalStateException> {
            HttpFixtures.write(root, "https://example.com/a/b", 200, emptyMap(), "y".toByteArray())
        }
        assertTrue("needs" in error.message.orEmpty()) { error.message }
    }

    @Test
    @DisplayName("Every recorded endpoint has a committed fixture")
    fun everyEndpointRecorded() {
        val roots = HttpFixtures.defaultRoots()
        val missing = FixtureEndpoints.all(Paths.get("templates"))
            .map { it.url }
            .filter { !HttpFixtures.exists(roots, it) }
        assertTrue(missing.isEmpty()) {
            "No fixture for these URLs, run ./gradlew recordHttpFixtures:\n" + missing.joinToString("\n")
        }
    }

    @Test
    @DisplayName("Templates point at URLs the recorder knows about")
    fun templateEndpointsFound() {
        val urls = FixtureEndpoints.fromTemplates(Paths.get("templates")).map { it.url }
        assertTrue(urls.any { it.startsWith("https://plugins.gradle.org/") }) { urls.toString() }
        assertTrue("https://registry.npmjs.org/@fabricmc%2Fcli/latest" in urls) { urls.toString() }
    }
}
