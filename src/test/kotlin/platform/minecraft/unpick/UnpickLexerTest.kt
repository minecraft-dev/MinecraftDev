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

package com.demonwav.mcdev.platform.minecraft.unpick

import com.demonwav.mcdev.framework.testLexer
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

@DisplayName("Unpick Lexing Tests")
class UnpickLexerTest {

    @Test
    @DisplayName("Spec Example Lexing Test")
    fun specTest() = testLexer("lexer/fixtures/spec.unpick", UnpickLexerAdapter())

    @Test
    @DisplayName("Member Signatures Lexing Test")
    fun memberSignaturesTest() = testLexer("lexer/fixtures/member_signatures.unpick", UnpickLexerAdapter())

    @Test
    @DisplayName("Whitespace And Literals Lexing Test")
    fun whitespaceTest() = testLexer("lexer/fixtures/whitespace.unpick", UnpickLexerAdapter())

    @Test
    @DisplayName("Unsupported Version Lexing Test")
    fun unsupportedTest() = testLexer("lexer/fixtures/unsupported.unpick", UnpickLexerAdapter())
}
