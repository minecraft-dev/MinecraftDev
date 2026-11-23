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

import com.demonwav.mcdev.platform.neoforge.version.NeoGradleVersion
import com.demonwav.mcdev.util.SemanticVersion
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

@DisplayName("NeoGradle Version Fetcher Tests")
class NeoGradleVersionTest : FetcherTestBase() {

    private val url = "https://maven.neoforged.net/releases/net/neoforged/gradle/userdev/maven-metadata.xml"

    @Test
    @DisplayName("Versions are read from recorded metadata, newest first")
    fun recorded() {
        val versions = fetch { NeoGradleVersion.downloadData() }.getOrThrow().versions
        assertEquals(6, versions.size)
        assertEquals(SemanticVersion.parse("7.1.39"), versions.first())
        assertEquals(versions.sortedDescending(), versions)
        assertNothingLogged()
    }

    @Test
    @DisplayName("Unparseable versions are skipped and at most 50 are kept")
    fun filtered() {
        val versions = (1..60).map { "1.0.$it" } + "not a version"
        stubBody(url, mavenMetadata(*versions.toTypedArray()))
        val result = fetch { NeoGradleVersion.downloadData() }.getOrThrow().versions
        assertEquals(50, result.size)
        assertEquals(SemanticVersion.parse("1.0.60"), result.first())
    }

    @Test
    @DisplayName("Metadata with only unparseable versions finds none")
    fun noParseableVersions() {
        stubBody(url, mavenMetadata("not a version"))
        assertFindsNone { NeoGradleVersion.downloadData() }
    }

    @Test
    @DisplayName("Metadata with no versions finds none")
    fun empty() {
        stubBody(url, mavenMetadata())
        assertFindsNone { NeoGradleVersion.downloadData() }
    }

    @Test
    @DisplayName("Malformed XML fails quietly")
    fun malformed() {
        stubBody(url, MALFORMED_XML)
        assertFailsQuietly { NeoGradleVersion.downloadData() }
    }

    @Test
    @DisplayName("An HTTP error fails quietly")
    fun httpError() {
        stubServerError(url)
        assertFailsQuietly { NeoGradleVersion.downloadData() }
    }
}
