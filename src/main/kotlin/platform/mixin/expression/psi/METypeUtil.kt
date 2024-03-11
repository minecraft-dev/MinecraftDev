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

import com.demonwav.mcdev.platform.mixin.expression.gen.psi.MEArrayAccessExpression
import com.demonwav.mcdev.platform.mixin.expression.gen.psi.MEBinaryExpression
import com.demonwav.mcdev.platform.mixin.expression.gen.psi.MECastExpression
import com.demonwav.mcdev.platform.mixin.expression.gen.psi.MEExpression
import com.demonwav.mcdev.platform.mixin.expression.gen.psi.MEExpressionTypes
import com.demonwav.mcdev.platform.mixin.expression.gen.psi.MENameExpression
import com.demonwav.mcdev.platform.mixin.expression.gen.psi.MEParenthesizedExpression
import com.demonwav.mcdev.platform.mixin.expression.gen.psi.MEStatement
import com.demonwav.mcdev.platform.mixin.expression.gen.psi.METype
import com.demonwav.mcdev.platform.mixin.expression.meExpressionElementFactory
import com.intellij.patterns.ObjectPattern
import com.intellij.patterns.PatternCondition
import com.intellij.psi.PsiElement
import com.intellij.psi.util.parentOfType
import com.intellij.util.ProcessingContext

object METypeUtil {
    fun convertExpressionToType(expr: MEExpression): METype? {
        return if (isExpressionValidType(expr)) {
            expr.project.meExpressionElementFactory.createType(expr.text)
        } else {
            null
        }
    }

    private fun isExpressionValidType(expr: MEExpression): Boolean {
        var e = expr
        while (true) {
            when (e) {
                is MEArrayAccessExpression -> {
                    if (e.indexExpr != null || e.rightBracketToken == null) {
                        return false
                    }
                    e = e.arrayExpr
                }
                is MENameExpression -> return true
                else -> return false
            }
        }
    }

    fun isExpressionDirectlyInTypePosition(expr: MEExpression): Boolean {
        var e: PsiElement? = expr
        while (e != null) {
            val parent = e.parent
            when (parent) {
                is MEArrayAccessExpression -> {}
                is MEParenthesizedExpression -> {
                    val grandparent = parent.parent
                    return grandparent is MECastExpression && e == grandparent.castTypeExpr
                }
                is MEBinaryExpression -> {
                    return parent.operator == MEExpressionTypes.TOKEN_INSTANCEOF && e == parent.rightExpr
                }
                else -> return false
            }
            e = parent
        }

        return false
    }

    fun isExpressionInTypePosition(expr: MEExpression): Boolean {
        var e: PsiElement? = expr
        while (e != null) {
            val parent = e.parent
            when (parent) {
                is MEParenthesizedExpression -> {
                    val grandparent = parent.parent
                    if (grandparent is MECastExpression && e == grandparent.castTypeExpr) {
                        return true
                    }
                }
                is MEBinaryExpression -> {
                    if (parent.operator == MEExpressionTypes.TOKEN_INSTANCEOF && e == parent.rightExpr) {
                        return true
                    }
                }
                is MEStatement -> return false
            }
            e = parent
        }

        return false
    }

    fun <T : PsiElement, Self : ObjectPattern<T, Self>> ObjectPattern<T, Self>.inTypePosition(): Self =
        with(InTypePositionCondition)
    fun <T : PsiElement, Self : ObjectPattern<T, Self>> ObjectPattern<T, Self>.notInTypePosition(): Self =
        without(InTypePositionCondition)
    fun <T : PsiElement, Self : ObjectPattern<T, Self>> ObjectPattern<T, Self>.validType(): Self =
        with(ValidTypeCondition)

    private object InTypePositionCondition : PatternCondition<PsiElement>("inTypePosition") {
        override fun accepts(t: PsiElement, context: ProcessingContext?) =
            t.parentOfType<MEExpression>()?.let(::isExpressionInTypePosition) == true
    }

    private object ValidTypeCondition : PatternCondition<PsiElement>("validType") {
        override fun accepts(t: PsiElement, context: ProcessingContext?) =
            t.parentOfType<MEExpression>(withSelf = true)?.let(::isExpressionValidType) == true
    }
}
