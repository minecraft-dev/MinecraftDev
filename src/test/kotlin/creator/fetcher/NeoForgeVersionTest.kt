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

import com.demonwav.mcdev.platform.neoforge.version.NeoForgeVersion
import com.demonwav.mcdev.util.SemanticVersion
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

@DisplayName("NeoForge Version Fetcher Tests")
class NeoForgeVersionTest : FetcherTestBase() {

    private val url = "https://maven.neoforged.net/releases/net/neoforged/neoforge/maven-metadata.xml"

    private fun v(version: String) = SemanticVersion.parse(version)

    @Test
    @DisplayName("Versions are grouped by Minecraft version, newest first")
    fun recorded() {
        val neoForge = fetch { NeoForgeVersion.downloadData() }.getOrThrow()
        assertEquals(62, neoForge.versions.size)
        assertEquals(listOf(v("26.3"), v("26.2"), v("26.1.2"), v("1.21.11")), neoForge.sortedMcVersions.take(4))
        assertEquals(
            listOf(v("26.3.0.103"), v("26.3.0.102"), v("26.3.0.101")),
            neoForge.getNeoForgeVersions(v("26.3")),
        )
        assertEquals(listOf(v("21.1.266"), v("21.1.265"), v("21.1.264")), neoForge.getNeoForgeVersions(v("1.21.1")))
        assertEquals(listOf(v("21.0.167"), v("21.0.166"), v("21.0.165")), neoForge.getNeoForgeVersions(v("1.21")))
        assertNothingLogged()
    }

    @Test
    @DisplayName("Minecraft versions are derived from both NeoForge version schemes")
    fun minecraftVersion() {
        assertEquals("1.21.1", NeoForgeVersion.getMinecraftVersion("21.1.77"))
        assertEquals("1.21", NeoForgeVersion.getMinecraftVersion("21.0.167"))
        assertEquals("1.20.2", NeoForgeVersion.getMinecraftVersion("20.2.91"))
        assertEquals("26.1", NeoForgeVersion.getMinecraftVersion("26.1.0.19-beta"))
        assertEquals("26.1.1", NeoForgeVersion.getMinecraftVersion("26.1.1.6-beta"))
        assertEquals("26.1.2", NeoForgeVersion.getMinecraftVersion("26.1.2.124"))
        assertNull(NeoForgeVersion.getMinecraftVersion("26.1.19"))
        assertNull(NeoForgeVersion.getMinecraftVersion("snapshot.1"))
    }

    @Test
    @DisplayName("Metadata with no versions finds none")
    fun empty() {
        stubBody(url, mavenMetadata())
        assertFindsNone { NeoForgeVersion.downloadData() }
    }

    @Test
    @DisplayName("Malformed XML fails quietly")
    fun malformed() {
        stubBody(url, MALFORMED_XML)
        assertFailsQuietly { NeoForgeVersion.downloadData() }
    }

    @Test
    @DisplayName("An HTTP error fails quietly")
    fun httpError() {
        stubServerError(url)
        assertFailsQuietly { NeoForgeVersion.downloadData() }
    }
}
