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

import com.demonwav.mcdev.platform.fabric.util.FabricApiVersions
import com.demonwav.mcdev.util.SemanticVersion
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

@DisplayName("Fabric API Version Fetcher Tests")
class FabricApiVersionsTest : FetcherTestBase() {

    private val url = "https://api.modrinth.com/v2/project/P7dR8mSH/version"

    private fun version(number: String, gameVersion: String, filename: String = "fabric-api-$number.jar") =
        """{"version_number": "$number", "game_versions": ["$gameVersion"], "files": [{"filename": "$filename"}]}"""

    @Test
    @DisplayName("Versions are read from recorded data")
    fun recorded() {
        val versions = fetch { FabricApiVersions.downloadData() }.getOrThrow().versions
        assertEquals(49, versions.size)
        assertEquals(SemanticVersion.parse("0.166.2+26.4"), versions[0].version)
        assertEquals(listOf("26.4-snapshot-8"), versions[0].gameVersions)
        assertEquals(SemanticVersion.parse("0.166.2+26.3"), versions[1].version)
        assertEquals(listOf("26.3"), versions[1].gameVersions)
        assertNothingLogged()
    }

    @Test
    @DisplayName("Versions with files other than the Fabric API jar are skipped")
    fun otherFiles() {
        stubBody(
            url,
            "[" + version("0.100.0+1.21", "1.21", "fabric-api-deprecated.jar") + "," +
                version("0.99.0+1.21", "1.21", "something-else.jar") + "]",
        )
        val versions = fetch { FabricApiVersions.downloadData() }.getOrThrow().versions
        assertEquals(listOf(SemanticVersion.parse("0.100.0+1.21")), versions.map { it.version })
    }

    @Test
    @DisplayName("No versions finds none")
    fun empty() {
        stubBody(url, "[]")
        assertFindsNone { FabricApiVersions.downloadData() }
    }

    @Test
    @DisplayName("Only skipped versions finds none")
    fun onlySkipped() {
        stubBody(url, "[" + version("0.99.0+1.21", "1.21", "something-else.jar") + "]")
        assertFindsNone { FabricApiVersions.downloadData() }
    }

    @Test
    @DisplayName("A missing field fails quietly")
    fun missingField() {
        stubBody(url, """[{"version_number": "0.100.0+1.21", "files": []}]""")
        assertFailsQuietly { FabricApiVersions.downloadData() }
    }

    @Test
    @DisplayName("JSON of the wrong shape fails quietly")
    fun wrongShape() {
        stubBody(url, "{}")
        assertFailsQuietly { FabricApiVersions.downloadData() }
    }

    @Test
    @DisplayName("Malformed JSON fails quietly")
    fun malformed() {
        stubBody(url, MALFORMED_JSON)
        assertFailsQuietly { FabricApiVersions.downloadData() }
    }

    @Test
    @DisplayName("An HTTP error fails quietly")
    fun httpError() {
        stubServerError(url)
        assertFailsQuietly { FabricApiVersions.downloadData() }
    }
}
