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
import com.demonwav.mcdev.platform.mixin.expression.gen.psi.METype
import com.demonwav.mcdev.platform.mixin.expression.lmfType
import com.demonwav.mcdev.platform.mixin.handlers.desugar.DesugarUtil
import com.intellij.lang.ASTNode
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiMethodCallExpression
import com.llamalad7.mixinextras.expression.impl.flow.postprocessing.LMFInfo
import org.objectweb.asm.Handle
import org.objectweb.asm.Type

abstract class MEConstructorReferenceExpressionImplMixin(node: ASTNode) : MEExpressionImplMixin(node), MEExpression {
    override fun matchesJava(java: PsiElement, context: MESourceMatchContext): Boolean {
        if (java !is PsiMethodCallExpression) {
            return false
        }

        val indyData = DesugarUtil.getIndyData(java) ?: return false
        if (indyData.bsm.owner != "java/lang/invoke/LambdaMetafactory") {
            return false
        }
        if (indyData.lmfType(java) != LMFInfo.Type.INSTANTIATION) {
            return false
        }

        val implMethod = indyData.bsmArgs.getOrNull(1) as? Handle ?: return false
        return className.matches(Type.getObjectType(implMethod.owner), context)
    }

    override fun getInputExprs() = emptyList<MEExpression>()

    abstract val className: METype
}
