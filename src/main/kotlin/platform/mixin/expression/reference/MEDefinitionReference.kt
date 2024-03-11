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

package com.demonwav.mcdev.platform.mixin.expression.reference

import com.demonwav.mcdev.platform.mixin.expression.gen.psi.MEName
import com.demonwav.mcdev.platform.mixin.expression.meExpressionElementFactory
import com.demonwav.mcdev.platform.mixin.expression.psi.MEExpressionFile
import com.intellij.openapi.util.TextRange
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiReference
import com.intellij.psi.util.parentOfType
import com.intellij.util.ArrayUtilRt
import com.intellij.util.IncorrectOperationException

class MEDefinitionReference(private var name: MEName) : PsiReference {
    override fun getElement() = name

    override fun getRangeInElement() = TextRange(0, name.textLength)

    override fun resolve(): PsiElement? {
        val file = element.parentOfType<MEExpressionFile>() ?: return null
        val name = element.text
        for (declItem in file.declarations) {
            val declaration = declItem.declaration
            if (declaration?.name == name) {
                return declaration
            }
        }

        return null
    }

    override fun getCanonicalText(): String = name.text

    override fun handleElementRename(newElementName: String): PsiElement {
        name = name.replace(name.project.meExpressionElementFactory.createName(newElementName)) as MEName
        return name
    }

    override fun bindToElement(element: PsiElement): PsiElement {
        throw IncorrectOperationException()
    }

    override fun isReferenceTo(element: PsiElement) = element.manager.areElementsEquivalent(element, resolve())

    override fun isSoft() = false

    override fun getVariants(): Array<Any> {
        return (name.containingFile as? MEExpressionFile)?.declarations?.mapNotNull { it.declaration }?.toTypedArray()
            ?: ArrayUtilRt.EMPTY_OBJECT_ARRAY
    }
}
