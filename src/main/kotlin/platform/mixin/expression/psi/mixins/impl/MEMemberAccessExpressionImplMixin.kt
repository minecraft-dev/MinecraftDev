/*
 * Minecraft Development for IntelliJ
 *
 * https://mcdev.io/
 *
 * Copyright (C) 2025 minecraft-dev
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
import com.demonwav.mcdev.platform.mixin.expression.gen.psi.MEName
import com.demonwav.mcdev.platform.mixin.expression.gen.psi.impl.MEExpressionImpl
import com.demonwav.mcdev.platform.mixin.expression.matchesFlow
import com.demonwav.mcdev.platform.mixin.handlers.injectionPoint.QualifiedMember
import com.intellij.lang.ASTNode
import com.intellij.psi.JavaPsiFacade
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiField
import com.intellij.psi.PsiModifier
import com.intellij.psi.PsiReferenceExpression
import com.siyeh.ig.psiutils.ExpressionUtils

abstract class MEMemberAccessExpressionImplMixin(node: ASTNode) : MEExpressionImpl(node) {
    override fun matchesJava(java: PsiElement, context: MESourceMatchContext): Boolean {
        if (java !is PsiReferenceExpression) {
            return false
        }

        val arrayFromLength = ExpressionUtils.getArrayFromLengthExpression(java)
        if (arrayFromLength != null) {
            if (memberName.isWildcard || memberName.text == "length") {
                return true
            }
        }

        val resolved = java.resolve() as? PsiField ?: return false
        if (resolved.hasModifierProperty(PsiModifier.STATIC)) {
            return false
        }

        val javaReceiver = java.qualifierExpression
            ?: JavaPsiFacade.getElementFactory(context.project).createExpressionFromText("this", null)
        context.fakeElementScope(java.qualifierExpression == null, java) {
            if (!javaReceiver.matchesFlow(receiverExpr, context)) {
                return false
            }
        }

        val qualifier = QualifiedMember.resolveQualifier(java) ?: resolved.containingClass ?: return false
        return context.getFields(memberName.text).any { it.matchField(resolved, qualifier) }
    }

    override fun getInputExprs() = listOf(receiverExpr)

    protected abstract val receiverExpr: MEExpression
    protected abstract val memberName: MEName
}
