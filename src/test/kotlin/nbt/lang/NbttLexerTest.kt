/*
 * Minecraft Development for IntelliJ
 *
 * https://mcdev.io/
 *
 * Copyright (C) 2024 minecraft-dev
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

package com.demonwav.mcdev.nbt.lang

import com.demonwav.mcdev.framework.testLexer
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

@DisplayName("NBTT Lexing Tests")
class NbttLexerTest {

    @Test
    @DisplayName("All Types Lexing Test")
    fun allTypesTest() = testLexer("lexer/fixtures/all_types.nbtt", NbttLexerAdapter())
}
