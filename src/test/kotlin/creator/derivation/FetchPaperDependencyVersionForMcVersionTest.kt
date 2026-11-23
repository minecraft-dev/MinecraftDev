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

package com.demonwav.mcdev.creator.derivation

import com.demonwav.mcdev.creator.custom.derivation.FetchPaperDependencyVersionForMcVersion
import com.demonwav.mcdev.util.SemanticVersion
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

@DisplayName("Fetch Paper Dependency Version Derivation Tests")
class FetchPaperDependencyVersionForMcVersionTest {

    private fun derive(mcVersion: String, buildSystem: String) =
        FetchPaperDependencyVersionForMcVersion().derive(listOf(SemanticVersion.parse(mcVersion), buildSystem))

    @Test
    @DisplayName("Versions before 26.1 use the R0.1 snapshot for every build system")
    fun beforeYearVersions() {
        assertEquals("1.20.4-R0.1-SNAPSHOT", derive("1.20.4", "Gradle"))
        assertEquals("1.21.11-R0.1-SNAPSHOT", derive("1.21.11", "Gradle"))
        assertEquals("1.21.11-R0.1-SNAPSHOT", derive("1.21.11", "Maven"))
    }

    @Test
    @DisplayName("Versions from 26.1 on use the latest build, in each build system's range syntax")
    fun yearVersions() {
        assertEquals("26.1.build.+", derive("26.1", "Gradle"))
        assertEquals("[26.1.build,)", derive("26.1", "Maven"))
        assertEquals("26.1.2.build.+", derive("26.1.2", "Gradle"))
        assertEquals("[26.1.2.build,)", derive("26.1.2", "Maven"))
    }
}
