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
import com.demonwav.mcdev.platform.mixin.expression.gen.psi.METype
import com.demonwav.mcdev.platform.mixin.expression.gen.psi.impl.MEExpressionImpl
import com.intellij.lang.ASTNode
import com.intellij.psi.JavaPsiFacade
import com.intellij.psi.PsiClassObjectAccessExpression
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiField
import com.intellij.psi.PsiReferenceExpression
import com.intellij.psi.util.PsiTypesUtil

abstract class MEClassConstantExpressionImplMixin(node: ASTNode) : MEExpressionImpl(node) {

    override fun matchesJava(java: PsiElement, context: MESourceMatchContext): Boolean {
        return when (java) {
            is PsiClassObjectAccessExpression -> type.matchesJava(java.operand.type, context)
            is PsiReferenceExpression -> {
                if (java.referenceName != "TYPE") {
                    return false
                }
                val field = java.resolve() as? PsiField ?: return false
                val containingClass = field.containingClass?.qualifiedName ?: return false
                val unboxedType = PsiTypesUtil.unboxIfPossible(containingClass)
                if (unboxedType == null || unboxedType == containingClass) {
                    return false
                }
                val javaType = JavaPsiFacade.getElementFactory(context.project).createPrimitiveTypeFromText(unboxedType)
                type.matchesJava(javaType, context)
            }
            else -> false
        }
    }

    override fun getInputExprs() = emptyList<MEExpression>()

    protected abstract val type: METype
}
