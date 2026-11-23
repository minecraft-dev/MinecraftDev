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

import com.demonwav.mcdev.creator.custom.derivation.RecommendJavaVersionForMcVersionPropertyDerivation
import com.demonwav.mcdev.creator.custom.model.ForgeVersions
import com.demonwav.mcdev.util.SemanticVersion
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

@DisplayName("Recommend Java Version Derivation Tests")
class RecommendJavaVersionForMcVersionPropertyDerivationTest {

    private val derivation = RecommendJavaVersionForMcVersionPropertyDerivation(17)

    @Test
    @DisplayName("Each Minecraft version recommends the Java version it requires")
    fun recommends() {
        val cases = mapOf(
            "1.12.2" to 8,
            "1.16.5" to 8,
            "1.17" to 16,
            "1.17.1" to 16,
            "1.18" to 17,
            "1.20.4" to 17,
            "1.20.5" to 21,
            "1.21.11" to 21,
            "26.1" to 25,
            "26.3" to 25,
        )
        for ((mcVersion, java) in cases) {
            assertEquals(java, derivation.derive(listOf(SemanticVersion.parse(mcVersion))), "Minecraft $mcVersion")
        }
    }

    @Test
    @DisplayName("Values with a Minecraft version are supported")
    fun hasMinecraftVersion() {
        val forge = ForgeVersions(SemanticVersion.parse("1.20.1"), SemanticVersion.parse("47.1.0"))
        assertEquals(17, derivation.derive(listOf(forge)))
    }

    @Test
    @DisplayName("Other values give the default")
    fun default() {
        assertEquals(17, derivation.derive(listOf("1.21")))
        assertEquals(17, derivation.derive(listOf(null)))
    }
}
