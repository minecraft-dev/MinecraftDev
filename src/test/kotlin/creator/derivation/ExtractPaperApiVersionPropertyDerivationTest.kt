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

import com.demonwav.mcdev.creator.custom.derivation.ExtractPaperApiVersionPropertyDerivation
import com.demonwav.mcdev.util.SemanticVersion
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

@DisplayName("Extract Paper API Version Derivation Tests")
class ExtractPaperApiVersionPropertyDerivationTest {

    @Test
    @DisplayName("Versions from 1.20.5 on are kept whole, and older ones are cut to major and minor")
    fun extracts() {
        val cases = mapOf(
            "1.18.2" to "1.18",
            "1.20.4" to "1.20",
            "1.20.5" to "1.20.5",
            "1.21.11" to "1.21.11",
            "26.1" to "26.1",
            "26.1.2" to "26.1.2",
        )
        for ((version, expected) in cases) {
            val result = ExtractPaperApiVersionPropertyDerivation().derive(listOf(SemanticVersion.parse(version)))
            assertEquals(SemanticVersion.parse(expected), result, version)
        }
    }
}
