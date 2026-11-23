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

import com.demonwav.mcdev.platform.fabric.util.FabricVersions
import com.demonwav.mcdev.util.SemanticVersion
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

@DisplayName("Fabric Version Fetcher Tests")
class FabricVersionsTest : FetcherTestBase() {

    private val url = "https://meta.fabricmc.net/v2/versions"

    @Test
    @DisplayName("Game, Yarn and loader versions are read from recorded data")
    fun recorded() {
        val versions = fetch { FabricVersions.downloadData() }.getOrThrow()
        assertEquals(58, versions.game.size)
        assertEquals("26.4-snapshot-8", versions.game.first().version)
        assertFalse(versions.game.first().stable)
        assertEquals(77, versions.mappings.size)
        val mapping = versions.mappings.first()
        assertEquals("1.21.11", mapping.gameVersion)
        assertEquals("1.21.11+build.6", mapping.version.name)
        assertEquals(6, mapping.version.build)
        assertEquals(10, versions.loader.size)
        assertEquals(SemanticVersion.parse("0.20.1"), versions.loader.first())
        assertNothingLogged()
    }

    @Test
    @DisplayName("No game versions finds none")
    fun noGameVersions() {
        stubBody(url, """{"game": [], "mappings": [], "loader": []}""")
        assertFindsNone { FabricVersions.downloadData() }
    }

    @Test
    @DisplayName("A missing section fails quietly")
    fun missingSection() {
        stubBody(url, """{"game": [{"version": "1.21", "stable": true}], "loader": []}""")
        assertFailsQuietly { FabricVersions.downloadData() }
    }

    @Test
    @DisplayName("A missing field fails quietly")
    fun missingField() {
        stubBody(url, """{"game": [{"version": "1.21"}], "mappings": [], "loader": []}""")
        assertFailsQuietly { FabricVersions.downloadData() }
    }

    @Test
    @DisplayName("JSON of the wrong shape fails quietly")
    fun wrongShape() {
        stubBody(url, "[]")
        assertFailsQuietly { FabricVersions.downloadData() }
    }

    @Test
    @DisplayName("Malformed JSON fails quietly")
    fun malformed() {
        stubBody(url, MALFORMED_JSON)
        assertFailsQuietly { FabricVersions.downloadData() }
    }

    @Test
    @DisplayName("An HTTP error fails quietly")
    fun httpError() {
        stubServerError(url)
        assertFailsQuietly { FabricVersions.downloadData() }
    }
}
