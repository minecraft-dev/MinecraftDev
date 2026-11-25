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

import com.demonwav.mcdev.platform.minecraft.unpick.gen.psi.UnpickClassName
import com.demonwav.mcdev.platform.minecraft.unpick.gen.psi.UnpickFieldExpression
import com.demonwav.mcdev.platform.minecraft.unpick.psi.UnpickElementFactory
import com.demonwav.mcdev.platform.minecraft.unpick.psi.mixins.UnpickFieldNameMixin
import com.demonwav.mcdev.util.cached
import com.demonwav.mcdev.util.equivalentTo
import com.intellij.codeInsight.lookup.LookupElementBuilder
import com.intellij.extapi.psi.ASTWrapperPsiElement
import com.intellij.lang.ASTNode
import com.intellij.openapi.util.TextRange
import com.intellij.psi.JavaPsiFacade
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiField
import com.intellij.psi.PsiReference
import com.intellij.psi.util.PsiModificationTracker
import com.intellij.psi.util.PsiTreeUtil
import com.intellij.util.ArrayUtil

abstract class UnpickFieldNameImplMixin(node: ASTNode) : ASTWrapperPsiElement(node), UnpickFieldNameMixin {

    override val ownerClassName: UnpickClassName?
        get() = PsiTreeUtil.getChildOfType(parent, UnpickClassName::class.java)

    override fun resolveField(): PsiField? = cached(PsiModificationTracker.MODIFICATION_COUNT) {
        ownerClassName?.resolveClass()?.findFieldByName(text, true)
    }

    override fun getElement(): PsiElement = this

    override fun getReference(): PsiReference = this

    override fun resolve(): PsiElement? = resolveField()

    override fun getVariants(): Array<Any> {
        val ownerClassName = ownerClassName ?: return ArrayUtil.EMPTY_OBJECT_ARRAY
        val fields = ownerClassName.resolveClass()?.allFields.orEmpty().map { field ->
            LookupElementBuilder.create(field).withTypeText(field.type.presentableText)
        }
        // While the owner of a field expression is being typed, its last part is parsed as the field name, and the
        // rest is parsed as the owner even though it may be a package
        val packageMembers = if (parent is UnpickFieldExpression) {
            val psiPackage = JavaPsiFacade.getInstance(project).findPackage(ownerClassName.qualifiedName)
            val scope = resolveScope
            psiPackage?.getSubPackages(scope).orEmpty().map(LookupElementBuilder::create) +
                psiPackage?.getClasses(scope).orEmpty().map(LookupElementBuilder::create)
        } else {
            emptyList()
        }
        return (fields + packageMembers).toTypedArray()
    }

    override fun getRangeInElement(): TextRange = TextRange(0, textLength)

    override fun getCanonicalText(): String = text

    override fun handleElementRename(newElementName: String): PsiElement {
        firstChild.replace(UnpickElementFactory.createIdentifier(project, newElementName))
        return this
    }

    override fun bindToElement(element: PsiElement): PsiElement {
        if (element is PsiField) {
            return handleElementRename(element.name)
        }
        return this
    }

    override fun isReferenceTo(element: PsiElement): Boolean {
        return element is PsiField && element equivalentTo resolve()
    }

    override fun isSoft(): Boolean = false
}
