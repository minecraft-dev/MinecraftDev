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

import com.demonwav.mcdev.creator.custom.types.PaperVersionCreatorProperty
import com.demonwav.mcdev.util.SemanticVersion
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

@DisplayName("Paper Version Fetcher Tests")
class PaperVersionsTest : FetcherTestBase() {

    private val url = "https://fill.papermc.io/v3/projects/paper"

    private fun v(version: String) = SemanticVersion.parse(version)

    @BeforeEach
    fun clearPaperCache() {
        PaperVersionCreatorProperty.clearCache()
    }

    @Test
    @DisplayName("Release versions from 1.18.2 on are listed newest first, including 26.x")
    fun recorded() {
        val versions = fetch { PaperVersionCreatorProperty.getPaperVersions() }!!
        assertEquals(27, versions.size)
        assertEquals(listOf(v("26.3"), v("26.2"), v("26.1.2"), v("26.1.1"), v("1.21.11")), versions.take(5))
        assertEquals(v("1.18.2"), versions.last())
        assertNothingLogged()
    }

    @Test
    @DisplayName("Pre-releases, release candidates and unparseable versions are left out")
    fun releasesOnly() {
        stubBody(
            url,
            """{"versions": {"26.1": ["26.1.1", "26.1-rc-2", "nonsense version"], "1.21": ["1.21.11-pre5", "1.21.11"]}}""",
        )
        val versions = fetch { PaperVersionCreatorProperty.getPaperVersions() }!!
        assertEquals(listOf(v("26.1.1"), v("1.21.11")), versions)
    }

    @Test
    @DisplayName("Successful results are cached")
    fun cached() {
        fetch { PaperVersionCreatorProperty.getPaperVersions() }!!
        fetch { PaperVersionCreatorProperty.getPaperVersions() }!!
        assertEquals(1, http.server.requestsTo(url).size)
    }

    @Test
    @DisplayName("Failures aren't cached")
    fun failureNotCached() {
        stubServerError(url)
        assertFailsQuietly { PaperVersionCreatorProperty.getPaperVersions() }
        stub(url, http.server.fixture(url))
        assertEquals(27, fetch { PaperVersionCreatorProperty.getPaperVersions() }!!.size)
    }

    @Test
    @DisplayName("Only versions older than 1.18.2 fails quietly")
    fun onlyOldVersions() {
        stubBody(url, """{"versions": {"1.18": ["1.18.1", "1.18"]}}""")
        assertFailsQuietly { PaperVersionCreatorProperty.getPaperVersions() }
    }

    @Test
    @DisplayName("No versions fails quietly")
    fun empty() {
        stubBody(url, """{"versions": {}}""")
        assertFailsQuietly { PaperVersionCreatorProperty.getPaperVersions() }
    }

    @Test
    @DisplayName("A missing versions object fails quietly")
    fun missingVersions() {
        stubBody(url, """{"project": {"id": "paper"}}""")
        assertFailsQuietly { PaperVersionCreatorProperty.getPaperVersions() }
    }

    @Test
    @DisplayName("Malformed JSON fails quietly")
    fun malformed() {
        stubBody(url, MALFORMED_JSON)
        assertFailsQuietly { PaperVersionCreatorProperty.getPaperVersions() }
    }

    @Test
    @DisplayName("An HTTP error fails quietly")
    fun httpError() {
        stubServerError(url)
        assertFailsQuietly { PaperVersionCreatorProperty.getPaperVersions() }
    }
}
