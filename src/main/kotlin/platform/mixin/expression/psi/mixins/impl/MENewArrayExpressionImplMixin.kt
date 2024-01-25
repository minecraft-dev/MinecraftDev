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

import com.demonwav.mcdev.platform.mixin.expression.gen.psi.MEExpression
import com.demonwav.mcdev.platform.mixin.expression.gen.psi.MEExpressionTypes
import com.demonwav.mcdev.platform.mixin.expression.gen.psi.impl.MEExpressionImpl
import com.demonwav.mcdev.platform.mixin.expression.psi.mixins.MENewArrayExpressionMixin
import com.intellij.lang.ASTNode
import com.intellij.psi.PsiElement
import com.intellij.psi.util.siblings

abstract class MENewArrayExpressionImplMixin(node: ASTNode) : MEExpressionImpl(node), MENewArrayExpressionMixin {
    override val dimensions get() = findChildrenByType<PsiElement>(MEExpressionTypes.TOKEN_LEFT_BRACKET).size

    override val dimExprTokens: List<MENewArrayExpressionMixin.DimExprTokens> get() {
        val result = mutableListOf<MENewArrayExpressionMixin.DimExprTokens>()

        var leftBracket: PsiElement? = findNotNullChildByType(MEExpressionTypes.TOKEN_LEFT_BRACKET)
        while (leftBracket != null) {
            var expr: MEExpression? = null
            var rightBracket: PsiElement? = null
            var nextLeftBracket: PsiElement? = null
            for (child in leftBracket.siblings(withSelf = false)) {
                if (child is MEExpression) {
                    expr = child
                } else {
                    when (child.node.elementType) {
                        MEExpressionTypes.TOKEN_RIGHT_BRACKET -> rightBracket = child
                        MEExpressionTypes.TOKEN_LEFT_BRACKET -> {
                            nextLeftBracket = child
                            break
                        }
                    }
                }
            }
            result += MENewArrayExpressionMixin.DimExprTokens(leftBracket, expr, rightBracket)
            leftBracket = nextLeftBracket
        }

        return result
    }
}
