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

package com.demonwav.mcdev.platform.mixin.expression.psi

import com.demonwav.mcdev.platform.mixin.expression.gen.psi.MEExpressionTypes
import com.intellij.psi.tree.TokenSet

object MEExpressionTokenSets {
    val STRINGS = TokenSet.create(
        MEExpressionTypes.TOKEN_STRING,
        MEExpressionTypes.TOKEN_STRING_ESCAPE,
        MEExpressionTypes.TOKEN_STRING_TERMINATOR,
    )
    val NUMBERS = TokenSet.create(
        MEExpressionTypes.TOKEN_INT_LIT,
        MEExpressionTypes.TOKEN_DEC_LIT,
    )
    val KEYWORDS = TokenSet.create(
        MEExpressionTypes.TOKEN_BOOL_LIT,
        MEExpressionTypes.TOKEN_NULL_LIT,
        MEExpressionTypes.TOKEN_DO,
        MEExpressionTypes.TOKEN_INSTANCEOF,
        MEExpressionTypes.TOKEN_NEW,
        MEExpressionTypes.TOKEN_RETURN,
        MEExpressionTypes.TOKEN_THROW,
        MEExpressionTypes.TOKEN_THIS,
        MEExpressionTypes.TOKEN_SUPER,
        MEExpressionTypes.TOKEN_CLASS,
        MEExpressionTypes.TOKEN_RESERVED,
    )
    val OPERATORS = TokenSet.create(
        MEExpressionTypes.TOKEN_BITWISE_NOT,
        MEExpressionTypes.TOKEN_MULT,
        MEExpressionTypes.TOKEN_DIV,
        MEExpressionTypes.TOKEN_MOD,
        MEExpressionTypes.TOKEN_PLUS,
        MEExpressionTypes.TOKEN_MINUS,
        MEExpressionTypes.TOKEN_SHL,
        MEExpressionTypes.TOKEN_SHR,
        MEExpressionTypes.TOKEN_USHR,
        MEExpressionTypes.TOKEN_LT,
        MEExpressionTypes.TOKEN_LE,
        MEExpressionTypes.TOKEN_GT,
        MEExpressionTypes.TOKEN_GE,
        MEExpressionTypes.TOKEN_EQ,
        MEExpressionTypes.TOKEN_NE,
        MEExpressionTypes.TOKEN_BITWISE_AND,
        MEExpressionTypes.TOKEN_BITWISE_XOR,
        MEExpressionTypes.TOKEN_BITWISE_OR,
        MEExpressionTypes.TOKEN_ASSIGN,
    )
    val PARENS = TokenSet.create(MEExpressionTypes.TOKEN_LEFT_PAREN, MEExpressionTypes.TOKEN_RIGHT_PAREN)
    val BRACKETS = TokenSet.create(MEExpressionTypes.TOKEN_LEFT_BRACKET, MEExpressionTypes.TOKEN_RIGHT_BRACKET)
    val BRACES = TokenSet.create(MEExpressionTypes.TOKEN_LEFT_BRACE, MEExpressionTypes.TOKEN_RIGHT_BRACE)
}
