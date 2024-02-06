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
import com.demonwav.mcdev.platform.mixin.expression.gen.psi.MEParenthesizedExpression
import com.demonwav.mcdev.platform.mixin.expression.gen.psi.impl.MEExpressionImpl
import com.demonwav.mcdev.platform.mixin.expression.psi.METypeUtil
import com.demonwav.mcdev.platform.mixin.expression.psi.mixins.MECastExpressionMixin
import com.demonwav.mcdev.platform.mixin.expression.psi.mixins.MEPsiUtil
import com.intellij.lang.ASTNode
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiInstanceOfExpression
import com.intellij.psi.PsiTypeCastExpression
import com.intellij.psi.PsiTypeTestPattern
import com.intellij.psi.util.JavaPsiPatternUtil

abstract class MECastExpressionImplMixin(node: ASTNode) : MEExpressionImpl(node), MECastExpressionMixin {
    override val castType get() = castTypeExpr?.let(METypeUtil::convertExpressionToType)
    override val castTypeExpr get() =
        (expressionList.let { it.getOrNull(it.size - 2) } as? MEParenthesizedExpression)?.expression
    override val castedExpr get() = expressionList.lastOrNull()

    override fun matchesJava(java: PsiElement, context: MESourceMatchContext): Boolean {
        return when (java) {
            is PsiTypeCastExpression -> {
                val javaType = java.castType?.type ?: return false
                val javaOperand = java.operand ?: return false
                castType?.matchesJava(javaType, context) == true &&
                    castedExpr?.matchesJava(javaOperand, context) == true
            }
            is PsiInstanceOfExpression -> {
                val pattern = JavaPsiPatternUtil.skipParenthesizedPatternDown(java.pattern) as? PsiTypeTestPattern
                    ?: return false
                val javaType = pattern.checkType?.type ?: return false
                val castedExpr = this.castedExpr ?: return false
                return MEPsiUtil.isWildcardExpression(castedExpr) && castType?.matchesJava(javaType, context) == true
            }
            else -> false
        }
    }

    protected abstract val expressionList: List<MEExpression>
}
