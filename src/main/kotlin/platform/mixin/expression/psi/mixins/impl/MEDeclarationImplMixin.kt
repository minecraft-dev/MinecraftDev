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

import com.demonwav.mcdev.asset.PlatformAssets
import com.demonwav.mcdev.platform.mixin.expression.gen.psi.MEDeclarationItem
import com.demonwav.mcdev.platform.mixin.expression.gen.psi.impl.MEItemImpl
import com.demonwav.mcdev.platform.mixin.expression.meExpressionElementFactory
import com.intellij.lang.ASTNode
import com.intellij.navigation.ItemPresentation
import com.intellij.openapi.util.Iconable
import com.intellij.psi.NavigatablePsiElement
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiNameIdentifierOwner
import com.intellij.psi.PsiNamedElement
import com.intellij.psi.search.LocalSearchScope
import com.intellij.util.PlatformIcons
import javax.swing.Icon

abstract class MEDeclarationImplMixin(
    node: ASTNode
) : MEItemImpl(node), PsiNamedElement, PsiNameIdentifierOwner, NavigatablePsiElement {
    override fun getName(): String = nameIdentifier.text

    override fun setName(name: String): PsiElement {
        this.nameIdentifier.replace(project.meExpressionElementFactory.createIdentifier(name))
        return this
    }

    override fun getNameIdentifier(): PsiElement = firstChild

    override fun getUseScope() = containingFile?.let(::LocalSearchScope) ?: super.getUseScope()

    override fun getPresentation() = object : ItemPresentation {
        override fun getPresentableText() = name

        override fun getIcon(unused: Boolean) = this@MEDeclarationImplMixin.getIcon(Iconable.ICON_FLAG_VISIBILITY)
    }

    override fun getIcon(flags: Int): Icon = if ((parent as? MEDeclarationItem)?.isType == true) {
        PlatformIcons.CLASS_ICON
    } else {
        PlatformAssets.MIXIN_ICON
    }
}
