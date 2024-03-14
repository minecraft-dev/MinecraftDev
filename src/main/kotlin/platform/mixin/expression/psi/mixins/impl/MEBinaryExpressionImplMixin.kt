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

package com.demonwav.mcdev.platform.mixin.expression.psi.mixins.impl

import com.demonwav.mcdev.platform.mixin.expression.MESourceMatchContext
import com.demonwav.mcdev.platform.mixin.expression.gen.psi.MEExpression
import com.demonwav.mcdev.platform.mixin.expression.gen.psi.MEExpressionTypes
import com.demonwav.mcdev.platform.mixin.expression.gen.psi.impl.MEExpressionImpl
import com.demonwav.mcdev.platform.mixin.expression.psi.METypeUtil
import com.demonwav.mcdev.platform.mixin.expression.psi.mixins.MEBinaryExpressionMixin
import com.intellij.lang.ASTNode
import com.intellij.psi.JavaTokenType
import com.intellij.psi.PsiBinaryExpression
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiInstanceOfExpression
import com.intellij.psi.PsiTypeTestPattern
import com.intellij.psi.tree.TokenSet
import com.intellij.psi.util.JavaPsiPatternUtil

abstract class MEBinaryExpressionImplMixin(node: ASTNode) : MEExpressionImpl(node), MEBinaryExpressionMixin {
    override val operator get() = node.findChildByType(operatorTokens)!!.elementType
    override val castType get() = rightExpr
        ?.takeIf { operator == MEExpressionTypes.TOKEN_INSTANCEOF }
        ?.let(METypeUtil::convertExpressionToType)

    override fun matchesJava(java: PsiElement, context: MESourceMatchContext): Boolean {
        if (operator == MEExpressionTypes.TOKEN_INSTANCEOF) {
            if (java !is PsiInstanceOfExpression) {
                return false
            }
            if (!leftExpr.matchesJava(java.operand, context)) {
                return false
            }
            val javaType = java.checkType?.type
                ?: (JavaPsiPatternUtil.skipParenthesizedPatternDown(java.pattern) as? PsiTypeTestPattern)
                    ?.checkType?.type
                ?: return false
            return castType?.matchesJava(javaType, context) == true
        } else {
            if (java !is PsiBinaryExpression) {
                return false
            }

            val operatorMatches = when (java.operationTokenType) {
                JavaTokenType.ASTERISK -> operator == MEExpressionTypes.TOKEN_MULT
                JavaTokenType.DIV -> operator == MEExpressionTypes.TOKEN_DIV
                JavaTokenType.PERC -> operator == MEExpressionTypes.TOKEN_MOD
                JavaTokenType.PLUS -> operator == MEExpressionTypes.TOKEN_PLUS
                JavaTokenType.MINUS -> operator == MEExpressionTypes.TOKEN_MINUS
                JavaTokenType.LTLT -> operator == MEExpressionTypes.TOKEN_SHL
                JavaTokenType.GTGT -> operator == MEExpressionTypes.TOKEN_SHR
                JavaTokenType.GTGTGT -> operator == MEExpressionTypes.TOKEN_USHR
                JavaTokenType.LT -> operator == MEExpressionTypes.TOKEN_LT
                JavaTokenType.LE -> operator == MEExpressionTypes.TOKEN_LE
                JavaTokenType.GT -> operator == MEExpressionTypes.TOKEN_GT
                JavaTokenType.GE -> operator == MEExpressionTypes.TOKEN_GE
                JavaTokenType.EQEQ -> operator == MEExpressionTypes.TOKEN_EQ
                JavaTokenType.NE -> operator == MEExpressionTypes.TOKEN_NE
                JavaTokenType.AND -> operator == MEExpressionTypes.TOKEN_BITWISE_AND
                JavaTokenType.XOR -> operator == MEExpressionTypes.TOKEN_BITWISE_XOR
                JavaTokenType.OR -> operator == MEExpressionTypes.TOKEN_BITWISE_OR
                else -> false
            }
            if (!operatorMatches) {
                return false
            }

            val javaLeft = java.lOperand
            val javaRight = java.rOperand ?: return false
            return leftExpr.matchesJava(javaLeft, context) && rightExpr?.matchesJava(javaRight, context) == true
        }
    }

    override fun getInputExprs() = if (operator == MEExpressionTypes.TOKEN_INSTANCEOF) {
        listOf(leftExpr)
    } else {
        listOfNotNull(leftExpr, rightExpr)
    }

    protected abstract val leftExpr: MEExpression
    protected abstract val rightExpr: MEExpression?

    companion object {
        private val operatorTokens = TokenSet.create(
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
            MEExpressionTypes.TOKEN_INSTANCEOF,
        )
    }
}
