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

import com.demonwav.mcdev.creator.custom.model.ForgeVersions
import com.demonwav.mcdev.platform.forge.version.ForgeVersion
import com.demonwav.mcdev.util.SemanticVersion
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

@DisplayName("Forge Version Fetcher Tests")
class ForgeVersionTest : FetcherTestBase() {

    private val url = "https://files.minecraftforge.net/maven/net/minecraftforge/forge/maven-metadata.xml"

    private fun v(version: String) = SemanticVersion.parse(version)

    @Test
    @DisplayName("Minecraft versions without a \"1.\" prefix are listed, and their next version keeps the prefix")
    fun noOnePrefix() {
        val forge = fetch { ForgeVersion.downloadData() }.getOrThrow()
        val mcVersions = forge.sortedMcVersions
        assertEquals(listOf(v("26.3"), v("26.2"), v("26.1.2"), v("26.1.1"), v("26.1")), mcVersions.take(5))
        assertEquals(listOf(v("62.0.9"), v("62.0.8"), v("62.0.5")), forge.getForgeVersions(v("26.1")))

        assertEquals("26.2", ForgeVersions(v("26.1"), v("62.0.9")).minecraftNext)
        assertEquals("26.2", ForgeVersions(v("26.1.2"), v("64.1.6")).minecraftNext)
        assertEquals("1.21", ForgeVersions(v("1.20.4"), v("49.0.1")).minecraftNext)
        assertNothingLogged()
    }

    @Test
    @DisplayName("Versions are read from recorded metadata")
    fun recorded() {
        val forge = fetch { ForgeVersion.downloadData() }.getOrThrow()
        assertEquals(233, forge.versions.size)
        assertEquals(v("66.1.8"), forge.getForgeVersions(v("26.3")).first())
        assertTrue(v("1.20.1") in forge.sortedMcVersions)
        assertNothingLogged()
    }

    @Test
    @DisplayName("Forge versions for a Minecraft version are newest first and capped at 50")
    fun forgeVersionOrder() {
        val versions = (1..60).map { "1.20.1-47.1.$it" } + "1.20-46.0.1"
        stubBody(url, mavenMetadata(*versions.toTypedArray()))
        val forge = fetch { ForgeVersion.downloadData() }.getOrThrow()
        val forgeVersions = forge.getForgeVersions(v("1.20.1"))
        assertEquals(50, forgeVersions.size)
        assertEquals(v("47.1.60"), forgeVersions.first())
        assertEquals(listOf(v("46.0.1")), forge.getForgeVersions(v("1.20")))
    }

    @Test
    @DisplayName("Versions without a Minecraft version are skipped")
    fun noMcVersion() {
        stubBody(url, mavenMetadata("47.1.0", "1.20.1-47.1.0"))
        val forge = fetch { ForgeVersion.downloadData() }.getOrThrow()
        assertEquals(listOf("1.20.1-47.1.0"), forge.versions)
        assertEquals(listOf(v("1.20.1")), forge.sortedMcVersions)
    }

    @Test
    @DisplayName("Metadata with no versions finds none")
    fun empty() {
        stubBody(url, mavenMetadata())
        assertFindsNone { ForgeVersion.downloadData() }
    }

    // Before every failure was caught, an XMLStreamException escaped the fetcher
    @Test
    @DisplayName("Malformed XML fails quietly")
    fun malformed() {
        stubBody(url, MALFORMED_XML)
        assertFailsQuietly { ForgeVersion.downloadData() }
    }

    @Test
    @DisplayName("An HTTP error fails quietly")
    fun httpError() {
        stubServerError(url)
        assertFailsQuietly { ForgeVersion.downloadData() }
    }
}
