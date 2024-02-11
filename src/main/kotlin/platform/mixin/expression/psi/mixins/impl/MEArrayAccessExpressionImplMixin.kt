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
import com.demonwav.mcdev.platform.mixin.expression.psi.MEPsiUtil
import com.demonwav.mcdev.platform.mixin.expression.psi.mixins.MEArrayAccessExpressionMixin
import com.intellij.lang.ASTNode
import com.intellij.psi.PsiArrayAccessExpression
import com.intellij.psi.PsiElement
import com.intellij.psi.util.PsiUtil

abstract class MEArrayAccessExpressionImplMixin(node: ASTNode) : MEExpressionImpl(node), MEArrayAccessExpressionMixin {
    override val leftBracketToken get() = findNotNullChildByType<PsiElement>(MEExpressionTypes.TOKEN_LEFT_BRACKET)
    override val rightBracketToken get() = findChildByType<PsiElement>(MEExpressionTypes.TOKEN_RIGHT_BRACKET)

    override fun matchesJava(java: PsiElement, context: MESourceMatchContext): Boolean {
        if (java !is PsiArrayAccessExpression) {
            return false
        }

        val readMatch = MEPsiUtil.isAccessedForReading(this) && PsiUtil.isAccessedForReading(java)
        val writeMatch = MEPsiUtil.isAccessedForWriting(this) && PsiUtil.isAccessedForWriting(java)
        if (!readMatch && !writeMatch) {
            return false
        }

        val javaArray = java.arrayExpression
        val javaIndex = java.indexExpression ?: return false
        return arrayExpr.matchesJava(javaArray, context) && indexExpr?.matchesJava(javaIndex, context) == true
    }

    protected abstract val arrayExpr: MEExpression
    protected abstract val indexExpr: MEExpression?
}
