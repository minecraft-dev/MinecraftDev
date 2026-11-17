/*
 * Minecraft Development for IntelliJ
 *
 * https://mcdev.io/
 *
 * Copyright (C) 2026 minecraft-dev
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

package com.demonwav.mcdev.platform.minecraft.unpick.psi.mixins.impl

import com.demonwav.mcdev.platform.minecraft.unpick.gen.psi.UnpickTypes
import com.demonwav.mcdev.platform.minecraft.unpick.psi.mixins.UnpickFieldExpressionMixin
import com.intellij.extapi.psi.ASTWrapperPsiElement
import com.intellij.lang.ASTNode
import com.intellij.psi.PsiElement

abstract class UnpickFieldExpressionImplMixin(node: ASTNode) : ASTWrapperPsiElement(node), UnpickFieldExpressionMixin {
    override val isWildcard: Boolean
        get() = wildcardElement != null

    override val wildcardElement: PsiElement?
        get() = node.findChildByType(UnpickTypes.STAR)?.psi

    override val isInstance: Boolean
        get() = node.getChildren(null).any { it.elementType == UnpickTypes.KEYWORD && it.text == "instance" }
}
