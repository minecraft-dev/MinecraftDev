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
import com.demonwav.mcdev.platform.mixin.expression.gen.psi.MEName
import com.demonwav.mcdev.platform.mixin.expression.psi.mixins.METypeMixin
import com.demonwav.mcdev.util.descriptor
import com.intellij.extapi.psi.ASTWrapperPsiElement
import com.intellij.lang.ASTNode
import com.intellij.psi.PsiArrayType
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiType

abstract class METypeImplMixin(node: ASTNode) : ASTWrapperPsiElement(node), METypeMixin {
    override val isArray get() = findChildByType<PsiElement>(MEExpressionTypes.TOKEN_LEFT_BRACKET) != null
    override val dimensions get() = findChildrenByType<PsiElement>(MEExpressionTypes.TOKEN_LEFT_BRACKET).size

    override fun matchesJava(java: PsiType, context: MESourceMatchContext): Boolean {
        if (MEName.isWildcard) {
            return java.arrayDimensions >= dimensions
        } else {
            var unwrappedElementType = java
            repeat(dimensions) {
                unwrappedElementType = (unwrappedElementType as? PsiArrayType)?.componentType ?: return false
            }
            val descriptor = unwrappedElementType.descriptor
            return context.getTypes(MEName.text).any { it == descriptor }
        }
    }

    @Suppress("PropertyName")
    protected abstract val MEName: MEName
}
