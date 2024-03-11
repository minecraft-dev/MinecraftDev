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

@file:JvmName("MEExpressionParserUtil")

package com.demonwav.mcdev.platform.mixin.expression.psi

import com.intellij.lang.PsiBuilder
import com.intellij.lang.parser.GeneratedParserUtilBase.* // ktlint-disable no-wildcard-imports

fun parseToRightBracket(
    builder: PsiBuilder,
    level: Int,
    recoverParser: Parser,
    rightBracketParser: Parser
): Boolean {
    recursion_guard_(builder, level, "parseToRightBracket")

    // continue over any stuff inside the brackets as error elements. We need to find our precious right bracket.
    var marker = enter_section_(builder, level, _NONE_)
    exit_section_(builder, level, marker, false, false, recoverParser)

    // consume our right bracket.
    marker = enter_section_(builder)
    val result = rightBracketParser.parse(builder, level)
    exit_section_(builder, marker, null, result)
    return result
}
