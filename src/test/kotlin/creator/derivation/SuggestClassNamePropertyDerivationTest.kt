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

import com.demonwav.mcdev.creator.custom.derivation.SuggestClassNamePropertyDerivation
import com.demonwav.mcdev.creator.custom.model.BuildSystemCoordinates
import com.demonwav.mcdev.creator.custom.model.ClassFqn
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

@DisplayName("Suggest Class Name Derivation Tests")
class SuggestClassNamePropertyDerivationTest {

    private val coords = BuildSystemCoordinates("com.example", "mod", "1.0")

    private fun suggest(name: String) = SuggestClassNamePropertyDerivation().derive(listOf(coords, name))

    @Test
    @DisplayName("The class is named after the project, in a package under the group")
    fun suggests() {
        assertEquals(ClassFqn("com.example.myMod.MyMod"), suggest("MyMod"))
    }

    @Test
    @DisplayName("Characters that can't be in a Java identifier split the name into capitalized words")
    fun sanitizes() {
        assertEquals(ClassFqn("com.example.myCoolMod.MyCoolMod"), suggest("my cool-mod!"))
        assertEquals(ClassFqn("com.example.my_mod.My_mod"), suggest("my_mod"))
    }
}
