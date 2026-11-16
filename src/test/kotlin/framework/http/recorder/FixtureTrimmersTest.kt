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

import com.google.gson.JsonParser
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

@DisplayName("HTTP Fixture Trimming Tests")
class FixtureTrimmersTest {

    private fun ByteArray.text() = toString(Charsets.UTF_8)

    @Test
    @DisplayName("Natural order compares numbers as numbers")
    fun naturalOrder() {
        val sorted = listOf("1.10", "1.9.4", "1.20.1-47.2.0", "1.20.1-47.10.0", "26.1-62.0.0", "1.2")
            .sortedWith(NaturalOrder)
        assertEquals(listOf("1.2", "1.9.4", "1.10", "1.20.1-47.2.0", "1.20.1-47.10.0", "26.1-62.0.0"), sorted)
    }

    @Test
    @DisplayName("JSON is pretty printed with sorted keys")
    fun prettyJson() {
        val output = Trim.PRETTY_JSON.apply("""[{"b": 1, "a": {"sha512": "x", "sha1": "y"}}]""".toByteArray())
        val expected = """
            [
              {
                "a": {
                  "sha1": "y",
                  "sha512": "x"
                },
                "b": 1
              }
            ]
        """.trimIndent() + "\n"
        assertEquals(expected, output.text())
    }

    @Test
    @DisplayName("Maven metadata keeps the newest versions of each group and nothing else changes")
    fun mavenMetadata() {
        val versions = listOf(
            "1.20.1-47.0.1", "1.20.1-47.10.0", "1.20.1-47.2.0", "1.20.1-47.3.0",
            "1.21-51.0.1",
            "26.1-62.0.1", "26.1-62.0.2",
        )
        val input = buildString {
            append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n")
            append("<metadata>\n  <version>top-level</version>\n  <versioning>\n")
            append("    <latest>26.1-62.0.2</latest>\n    <versions>\n")
            versions.forEach { append("      <version>$it</version>\n") }
            append("    </versions>\n  </versioning>\n</metadata>\n")
        }

        val output = MavenMetadataTrim(keepPerGroup = 2) { it.substringBefore('-') }.apply(input.toByteArray()).text()

        val kept = Regex("<version>([^<]*)</version>").findAll(output).map { it.groupValues[1] }.toList()
        assertEquals(
            listOf("top-level", "1.20.1-47.10.0", "1.20.1-47.3.0", "1.21-51.0.1", "26.1-62.0.1", "26.1-62.0.2"),
            kept,
        )
        val removedLines = versions.filter { it !in kept }.map { "      <version>$it</version>\n" }
        assertEquals(removedLines.fold(input) { text, line -> text.replace(line, "") }, output)
    }

    @Test
    @DisplayName("Versions written on one line stay on their own line")
    fun mavenMetadataOnOneLine() {
        val prefix = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n<metadata><versioning><versions>\n"
        val suffix = "\n</versions></versioning></metadata>\n"
        val input = prefix + "<version>1.0.1</version><version>1.0.2</version><version>1.0.3</version>" + suffix

        val output = MavenMetadataTrim(keepPerGroup = 1).apply(input.toByteArray()).text()

        assertEquals(prefix + "<version>1.0.3</version>" + suffix, output)
    }

    @Test
    @DisplayName("Metadata without a versions list is left alone")
    fun mavenMetadataWithoutVersions() {
        val input = "<metadata><version>1.0</version></metadata>".toByteArray()
        assertEquals(input.text(), MavenMetadataTrim().apply(input).text())
    }

    @Test
    @DisplayName("Fabric meta keeps stable game versions and their newest mappings")
    fun fabricMeta() {
        val input = """
            {
              "game": [
                {"version": "26.1-snapshot-2", "stable": false},
                {"version": "26.1-snapshot-1", "stable": false},
                {"version": "1.21.11", "stable": true},
                {"version": "1.16.5", "stable": true}
              ],
              "mappings": [
                {"gameVersion": "26.1-snapshot-1", "version": "26.1-snapshot-1+build.1", "build": 1},
                {"gameVersion": "1.21.11", "version": "1.21.11+build.3", "build": 3},
                {"gameVersion": "1.21.11", "version": "1.21.11+build.2", "build": 2},
                {"gameVersion": "1.21.11", "version": "1.21.11+build.1", "build": 1}
              ],
              "intermediary": [{"version": "26.1-snapshot-1"}, {"version": "1.16.5"}],
              "loader": [{"version": "0.17.0"}, {"version": "0.16.0"}, {"version": "0.15.0"}],
              "installer": [{"version": "1.1.0"}, {"version": "1.0.0"}]
            }
        """.trimIndent()

        val output = FabricMetaTrim(unstableGames = 1, mappingsPerGame = 2, loaders = 2, installers = 1)
            .apply(input.toByteArray())
        val json = JsonParser.parseString(output.text()).asJsonObject

        fun versions(key: String) = json.getAsJsonArray(key).map { it.asJsonObject["version"].asString }
        assertEquals(listOf("26.1-snapshot-2", "1.21.11", "1.16.5"), versions("game"))
        assertEquals(listOf("1.21.11+build.3", "1.21.11+build.2"), versions("mappings"))
        assertEquals(listOf("1.16.5"), versions("intermediary"))
        assertEquals(listOf("0.17.0", "0.16.0"), versions("loader"))
        assertEquals(listOf("1.1.0"), versions("installer"))
        assertTrue(output.text().contains("\n  \"game\": [\n")) { "Expected pretty-printed JSON" }
    }

    @Test
    @DisplayName("Modrinth keeps the newest version per release game version and loader")
    fun modrinth() {
        fun entry(number: String, date: String, games: String, loaders: String) =
            """{"version_number": "$number", "date_published": "$date", "downloads": 100, """ +
                """"game_versions": [$games], "loaders": [$loaders]}"""

        val input = listOf(
            entry("5", "2026-05", "\"26.1-snapshot-1\"", "\"fabric\""),
            entry("4", "2026-04", "\"1.21.1\"", "\"fabric\""),
            entry("3", "2026-03", "\"1.21.1\"", "\"neoforge\""),
            entry("2", "2026-02", "\"1.21\", \"1.21.1\"", "\"fabric\""),
            entry("1", "2026-01", "\"1.21\"", "\"fabric\""),
        ).joinToString(",", "[", "]")

        val output = ModrinthVersionsTrim(newest = 1).apply(input.toByteArray())
        val kept = JsonParser.parseString(output.text()).asJsonArray.map { it.asJsonObject }
        assertEquals(listOf("5", "4", "3", "2"), kept.map { it["version_number"].asString })
        assertTrue(kept.none { it.has("downloads") }) { "Download counts change on every request" }
    }
}
