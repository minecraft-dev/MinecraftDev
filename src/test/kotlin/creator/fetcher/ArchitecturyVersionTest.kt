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

import com.demonwav.mcdev.platform.architectury.ArchitecturyVersion
import com.demonwav.mcdev.util.SemanticVersion
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

@DisplayName("Architectury API Version Fetcher Tests")
class ArchitecturyVersionTest : FetcherTestBase() {

    private val url = "https://api.modrinth.com/v2/project/architectury-api/version"

    private fun v(version: String) = SemanticVersion.parse(version)

    @Test
    @DisplayName("Versions are grouped by Minecraft version, newest first, without the loader suffix")
    fun recorded() {
        val architectury = fetch { ArchitecturyVersion.downloadData() }.getOrThrow()
        assertEquals(33, architectury.versions.size)
        assertEquals(listOf(v("22.0.5")), architectury.getArchitecturyVersions(v("26.3")))
        assertEquals(listOf(v("20.1.17"), v("20.0.4")), architectury.getArchitecturyVersions(v("26.1.2")))
        assertEquals(listOf(v("13.0.12")), architectury.getArchitecturyVersions(v("1.21")))
        assertEquals(emptyList<SemanticVersion>(), architectury.getArchitecturyVersions(v("1.12.2")))
        assertNothingLogged()
    }

    @Test
    @DisplayName("Unparseable game versions are skipped")
    fun unparseableGameVersion() {
        stubBody(url, """[{"version_number": "13.0.1+fabric", "game_versions": ["not a version", "1.21"]}]""")
        val architectury = fetch { ArchitecturyVersion.downloadData() }.getOrThrow()
        assertEquals(mapOf(v("1.21") to listOf(v("13.0.1"))), architectury.versions)
    }

    @Test
    @DisplayName("No versions finds none")
    fun empty() {
        stubBody(url, "[]")
        assertFindsNone { ArchitecturyVersion.downloadData() }
    }

    @Test
    @DisplayName("An empty body fails quietly")
    fun emptyBody() {
        stubBody(url, "")
        assertFailsQuietly { ArchitecturyVersion.downloadData() }
    }

    @Test
    @DisplayName("An unparseable version number fails quietly")
    fun unparseableVersion() {
        stubBody(url, """[{"version_number": "not a version", "game_versions": ["1.21"]}]""")
        assertFailsQuietly { ArchitecturyVersion.downloadData() }
    }

    @Test
    @DisplayName("A missing field fails quietly")
    fun missingField() {
        stubBody(url, """[{"version_number": "13.0.1+fabric"}]""")
        assertFailsQuietly { ArchitecturyVersion.downloadData() }
    }

    @Test
    @DisplayName("JSON of the wrong shape fails quietly")
    fun wrongShape() {
        stubBody(url, "{}")
        assertFailsQuietly { ArchitecturyVersion.downloadData() }
    }

    @Test
    @DisplayName("Malformed JSON fails quietly")
    fun malformed() {
        stubBody(url, MALFORMED_JSON)
        assertFailsQuietly { ArchitecturyVersion.downloadData() }
    }

    @Test
    @DisplayName("An HTTP error fails quietly")
    fun httpError() {
        stubServerError(url)
        assertFailsQuietly { ArchitecturyVersion.downloadData() }
    }
}
