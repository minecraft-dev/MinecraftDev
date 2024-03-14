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
import com.demonwav.mcdev.platform.mixin.expression.gen.psi.MEArguments
import com.demonwav.mcdev.platform.mixin.expression.gen.psi.MEExpression
import com.demonwav.mcdev.platform.mixin.expression.gen.psi.MEName
import com.demonwav.mcdev.platform.mixin.expression.gen.psi.impl.MEExpressionImpl
import com.intellij.lang.ASTNode
import com.intellij.psi.JavaPsiFacade
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiMethodCallExpression
import com.intellij.psi.PsiModifier
import com.intellij.psi.util.PsiUtil
import com.siyeh.ig.psiutils.MethodCallUtils

abstract class MEMethodCallExpressionImplMixin(node: ASTNode) : MEExpressionImpl(node) {
    override fun matchesJava(java: PsiElement, context: MESourceMatchContext): Boolean {
        if (java !is PsiMethodCallExpression) {
            return false
        }

        if (MethodCallUtils.hasSuperQualifier(java)) {
            return false
        }

        if (!memberName.matchesJavaExpr(java, context)) {
            return false
        }

        val method = java.resolveMethod() ?: return false
        if (method.hasModifierProperty(PsiModifier.STATIC)) {
            return false
        }

        val javaReceiver = PsiUtil.skipParenthesizedExprDown(java.methodExpression.qualifierExpression)
            ?: JavaPsiFacade.getElementFactory(context.project).createExpressionFromText("this", null)
        context.fakeElementScope(java.methodExpression.qualifierExpression == null, java.methodExpression) {
            if (!receiverExpr.matchesJava(javaReceiver, context)) {
                return false
            }
        }

        return arguments?.matchesJava(java.argumentList, context) == true
    }

    override fun getInputExprs() = listOf(receiverExpr) + (arguments?.expressionList ?: emptyList())

    protected abstract val receiverExpr: MEExpression
    protected abstract val memberName: MEName
    protected abstract val arguments: MEArguments?
}
