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
import com.demonwav.mcdev.platform.mixin.expression.gen.psi.MEExpressionTypes
import com.demonwav.mcdev.platform.mixin.expression.psi.mixins.MENameMixin
import com.demonwav.mcdev.platform.mixin.util.LocalVariables
import com.intellij.extapi.psi.ASTWrapperPsiElement
import com.intellij.lang.ASTNode
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiField
import com.intellij.psi.PsiReferenceExpression
import com.intellij.psi.PsiVariable
import com.intellij.psi.util.PsiUtil

abstract class MENameImplMixin(node: ASTNode) : ASTWrapperPsiElement(node), MENameMixin {
    override val isWildcard get() = node.elementType == MEExpressionTypes.TOKEN_WILDCARD

    override fun matchesJavaExpr(javaExpr: PsiElement, context: MESourceMatchContext): Boolean {
        if (isWildcard) {
            return true
        }

        // match against elements targeted with @At
        val name = text
        if (javaExpr in context.getTargetedElements(name)) {
            return true
        }

        // match against local variables
        if (javaExpr !is PsiReferenceExpression) {
            return false
        }
        val variable = javaExpr.resolve() as? PsiVariable ?: return false
        if (variable is PsiField) {
            return false
        }

        val sourceArgs by lazy {
            LocalVariables.guessLocalsAt(javaExpr, true, !PsiUtil.isAccessedForWriting(javaExpr))
        }
        val sourceVariables by lazy {
            LocalVariables.guessLocalsAt(javaExpr, false, !PsiUtil.isAccessedForWriting(javaExpr))
        }
        for (localInfo in context.getLocalInfos(name)) {
            val sourceLocals = if (localInfo.argsOnly) sourceArgs else sourceVariables
            for (local in localInfo.matchSourceLocals(sourceLocals)) {
                if (local.variable == variable) {
                    return true
                }
            }
        }

        return false
    }
}
