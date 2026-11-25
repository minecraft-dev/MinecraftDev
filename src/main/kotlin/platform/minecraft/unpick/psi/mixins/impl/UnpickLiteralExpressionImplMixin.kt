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

import com.demonwav.mcdev.platform.minecraft.unpick.DataType
import com.demonwav.mcdev.platform.minecraft.unpick.UnpickLiterals
import com.demonwav.mcdev.platform.minecraft.unpick.gen.psi.UnpickNegateExpression
import com.demonwav.mcdev.platform.minecraft.unpick.psi.mixins.UnpickLiteralExpressionMixin
import com.intellij.extapi.psi.ASTWrapperPsiElement
import com.intellij.lang.ASTNode

abstract class UnpickLiteralExpressionImplMixin(node: ASTNode) :
    ASTWrapperPsiElement(node),
    UnpickLiteralExpressionMixin {
    override val isNegated: Boolean
        get() = parent is UnpickNegateExpression

    override val dataType: DataType?
        get() = UnpickLiterals.dataTypeOf(node.firstChildNode.elementType)

    override val parseResult: UnpickLiterals.Result
        get() = UnpickLiterals.parse(node.firstChildNode.elementType, text, isNegated)
}
