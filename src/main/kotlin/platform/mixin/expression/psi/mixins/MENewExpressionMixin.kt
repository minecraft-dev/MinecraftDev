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

package com.demonwav.mcdev.platform.mixin.expression.psi.mixins

import com.demonwav.mcdev.platform.mixin.expression.gen.psi.MEArguments
import com.demonwav.mcdev.platform.mixin.expression.gen.psi.MEExpression
import com.intellij.psi.PsiElement

interface MENewExpressionMixin : PsiElement {
    val isArrayCreation: Boolean
    val hasConstructorArguments: Boolean
    val dimensions: Int
    val dimExprTokens: List<DimExprTokens>
    val arrayInitializer: MEArguments?

    class DimExprTokens(val leftBracket: PsiElement, val expr: MEExpression?, val rightBracket: PsiElement?)
}
