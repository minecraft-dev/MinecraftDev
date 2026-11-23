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

import com.demonwav.mcdev.creator.custom.derivation.ReplacePropertyDerivation
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

@DisplayName("Replace Derivation Tests")
class ReplacePropertyDerivationTest {

    private fun replace(value: Any?, maxLength: Int? = null, lowercase: Boolean = false) =
        ReplacePropertyDerivation(Regex("[^a-zA-Z0-9_]+"), "_", maxLength, lowercase).derive(listOf(value))

    @Test
    @DisplayName("Matches are replaced")
    fun replaces() {
        assertEquals("My_Cool_Mod_", replace("My Cool-Mod!"))
    }

    @Test
    @DisplayName("The value can be lowercased before replacing")
    fun lowercase() {
        assertEquals("my_cool_mod", replace("My Cool Mod", lowercase = true))
    }

    @Test
    @DisplayName("The result is cut to the maximum length")
    fun maxLength() {
        assertEquals("My_Co", replace("My Cool Mod", maxLength = 5))
        assertEquals("My", replace("My", maxLength = 5))
    }

    @Test
    @DisplayName("A value that isn't a string gives null")
    fun notString() {
        assertNull(replace(42))
        assertNull(replace(null))
    }
}
