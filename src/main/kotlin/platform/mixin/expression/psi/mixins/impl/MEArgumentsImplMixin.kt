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
import com.demonwav.mcdev.platform.mixin.expression.psi.mixins.MEArgumentsMixin
import com.intellij.extapi.psi.ASTWrapperPsiElement
import com.intellij.lang.ASTNode
import com.intellij.psi.PsiExpression
import com.intellij.psi.util.PsiUtil

abstract class MEArgumentsImplMixin(node: ASTNode) : ASTWrapperPsiElement(node), MEArgumentsMixin {
    override fun matchesJava(java: Array<PsiExpression>, context: MESourceMatchContext): Boolean {
        val exprs = expressionList
        if (exprs.size != java.size) {
            return false
        }
        return exprs.asSequence().zip(java.asSequence()).all { (expr, javaExpr) ->
            val actualJavaExpr = PsiUtil.skipParenthesizedExprDown(javaExpr) ?: return@all false
            expr.matchesJava(actualJavaExpr, context)
        }
    }

    protected abstract val expressionList: List<MEExpression>
}
