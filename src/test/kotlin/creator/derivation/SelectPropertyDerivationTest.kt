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

import com.demonwav.mcdev.creator.custom.PropertyDerivationSelect
import com.demonwav.mcdev.creator.custom.derivation.SelectPropertyDerivation
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

@DisplayName("Select Derivation Tests")
class SelectPropertyDerivationTest {

    private val options = listOf(
        PropertyDerivationSelect($$"$BUILD_SYSTEM == 'gradle' && $USE_KOTLIN", "build.gradle.kts"),
        PropertyDerivationSelect($$"$BUILD_SYSTEM == 'gradle'", "build.gradle"),
        PropertyDerivationSelect($$"$BUILD_SYSTEM == 'maven'", "pom.xml"),
    )

    private val derivation = SelectPropertyDerivation(listOf("BUILD_SYSTEM", "USE_KOTLIN"), options, "none")

    @Test
    @DisplayName("The first option whose condition holds is selected")
    fun firstMatch() {
        assertEquals("build.gradle.kts", derivation.derive(listOf("gradle", true)))
        assertEquals("build.gradle", derivation.derive(listOf("gradle", false)))
        assertEquals("pom.xml", derivation.derive(listOf("maven", true)))
    }

    @Test
    @DisplayName("The default is selected when no condition holds")
    fun default() {
        assertEquals("none", derivation.derive(listOf("ant", false)))
    }

    @Test
    @DisplayName("Conditions can use the base properties without parents")
    fun noParents() {
        val options = listOf(PropertyDerivationSelect($$"$semver.parse('1.21') > $mcver.MC1_20", "new"))
        assertEquals("new", SelectPropertyDerivation(null, options, "old").derive(emptyList()))
    }
}
