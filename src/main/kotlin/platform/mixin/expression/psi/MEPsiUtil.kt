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

import com.demonwav.mcdev.platform.mixin.expression.gen.psi.MEAssignStatement
import com.demonwav.mcdev.platform.mixin.expression.gen.psi.MEExpression
import com.demonwav.mcdev.platform.mixin.expression.gen.psi.MENameExpression
import com.demonwav.mcdev.platform.mixin.expression.gen.psi.MEParenthesizedExpression

object MEPsiUtil {
    fun isAccessedForReading(expr: MEExpression): Boolean {
        return !isAccessedForWriting(expr)
    }

    fun isAccessedForWriting(expr: MEExpression): Boolean {
        val parent = expr.parent
        return parent is MEAssignStatement && expr == parent.targetExpr
    }

    fun skipParenthesizedExprDown(expr: MEExpression): MEExpression? {
        var e: MEExpression? = expr
        while (e is MEParenthesizedExpression) {
            e = e.expression
        }
        return e
    }

    fun isWildcardExpression(expr: MEExpression): Boolean {
        val actualExpr = skipParenthesizedExprDown(expr) ?: return false
        return actualExpr is MENameExpression && actualExpr.meName.isWildcard
    }

    fun isIdentifierStart(char: Char): Boolean {
        return char in 'a'..'z' || char in 'A'..'Z' || char == '_'
    }

    fun isIdentifierPart(char: Char): Boolean {
        return isIdentifierStart(char) || char in '0'..'9'
    }
}
