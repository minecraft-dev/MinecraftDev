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

import com.demonwav.mcdev.platform.minecraft.unpick.psi.UnpickClassReference
import com.demonwav.mcdev.platform.minecraft.unpick.psi.mixins.UnpickPackageNameMixin
import com.intellij.extapi.psi.ASTWrapperPsiElement
import com.intellij.lang.ASTNode
import com.intellij.psi.PsiReference

abstract class UnpickPackageNameImplMixin(node: ASTNode) : ASTWrapperPsiElement(node), UnpickPackageNameMixin {
    override val qualifiedName: String
        get() = UnpickClassReference.qualifiedName(this)

    override fun getReferences(): Array<PsiReference> = UnpickClassReference.create(this, isPackage = true)

    override fun getReference(): PsiReference? = references.lastOrNull()
}
