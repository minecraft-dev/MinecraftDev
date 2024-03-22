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
import com.demonwav.mcdev.platform.mixin.expression.gen.psi.impl.MEStatementImpl
import com.intellij.lang.ASTNode
import com.intellij.psi.JavaTokenType
import com.intellij.psi.PsiAssignmentExpression
import com.intellij.psi.PsiElement
import com.intellij.psi.util.PsiUtil
import com.siyeh.ig.PsiReplacementUtil

abstract class MEAssignStatementImplMixin(node: ASTNode) : MEStatementImpl(node) {
    override fun matchesJava(java: PsiElement, context: MESourceMatchContext): Boolean {
        if (java !is PsiAssignmentExpression) {
            return false
        }
        val isOperatorAssignment = java.operationTokenType != JavaTokenType.EQ
        val expandedJava = if (isOperatorAssignment) {
            PsiReplacementUtil.replaceOperatorAssignmentWithAssignmentExpression(java.copy() as PsiAssignmentExpression)
                as PsiAssignmentExpression
        } else {
            java
        }

        val leftJava = PsiUtil.skipParenthesizedExprDown(expandedJava.lExpression) ?: return false
        val rightJava = PsiUtil.skipParenthesizedExprDown(expandedJava.rExpression) ?: return false
        context.fakeElementScope(isOperatorAssignment, java) {
            return targetExpr.matchesJava(leftJava, context) && rightExpr?.matchesJava(rightJava, context) == true
        }
    }

    override fun getInputExprs() = targetExpr.getInputExprs() + listOfNotNull(rightExpr)

    protected abstract val targetExpr: MEExpression
    protected abstract val rightExpr: MEExpression?
}
