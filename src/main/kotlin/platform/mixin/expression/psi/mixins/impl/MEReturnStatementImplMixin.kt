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
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiReturnStatement
import com.intellij.psi.util.PsiUtil

abstract class MEReturnStatementImplMixin(node: ASTNode) : MEStatementImpl(node) {
    override fun matchesJava(java: PsiElement, context: MESourceMatchContext): Boolean {
        if (java !is PsiReturnStatement) {
            return false
        }
        val javaReturnValue = PsiUtil.skipParenthesizedExprDown(java.returnValue) ?: return false
        return valueExpr?.matchesJava(javaReturnValue, context) == true
    }

    override fun getInputExprs() = listOfNotNull(valueExpr)

    protected abstract val valueExpr: MEExpression?
}
