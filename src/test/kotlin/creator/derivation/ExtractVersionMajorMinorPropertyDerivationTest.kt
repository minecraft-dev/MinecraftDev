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

import com.demonwav.mcdev.creator.custom.derivation.ExtractVersionMajorMinorPropertyDerivation
import com.demonwav.mcdev.util.SemanticVersion
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

@DisplayName("Extract Version Major Minor Derivation Tests")
class ExtractVersionMajorMinorPropertyDerivationTest {

    @Test
    @DisplayName("The major and minor versions are kept, without a pre-release suffix, or none for a single part")
    fun extracts() {
        val cases = mapOf(
            "1.21.11" to "1.21",
            "1.21" to "1.21",
            "1.21.5-pre1" to "1.21",
            "26.1.2" to "26.1",
            "1.21-pre1" to "1.21",
            "26.1-snapshot-1" to "26.1",
            "26.1-rc-2" to "26.1",
            "1" to "",
            "24w39a" to "",
        )
        for ((version, expected) in cases) {
            val result = ExtractVersionMajorMinorPropertyDerivation().derive(listOf(SemanticVersion.parse(version)))
            assertEquals(expected, result.toString(), version)
        }
    }
}
