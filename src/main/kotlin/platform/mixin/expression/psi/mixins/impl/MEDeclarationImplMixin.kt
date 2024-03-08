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

import com.demonwav.mcdev.platform.mixin.expression.gen.psi.impl.MEItemImpl
import com.demonwav.mcdev.platform.mixin.expression.meExpressionElementFactory
import com.demonwav.mcdev.platform.mixin.expression.psi.mixins.MEDeclarationMixin
import com.intellij.lang.ASTNode
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiNamedElement

abstract class MEDeclarationImplMixin(node: ASTNode) : MEItemImpl(node), MEDeclarationMixin, PsiNamedElement {
    override fun getName(): String = nameIdentifier.text

    override fun setName(name: String): PsiElement {
        this.nameIdentifier.replace(project.meExpressionElementFactory.createIdentifier(name))
        return this
    }

    override val nameIdentifier: PsiElement
        get() = firstChild
}
