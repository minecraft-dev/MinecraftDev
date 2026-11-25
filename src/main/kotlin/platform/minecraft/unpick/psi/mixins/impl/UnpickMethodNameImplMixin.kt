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
import com.demonwav.mcdev.platform.minecraft.unpick.gen.psi.UnpickTypes
import com.demonwav.mcdev.platform.minecraft.unpick.psi.UnpickElementFactory
import com.demonwav.mcdev.platform.minecraft.unpick.psi.mixins.UnpickMethodNameMixin
import com.demonwav.mcdev.util.MemberReference
import com.demonwav.mcdev.util.cached
import com.demonwav.mcdev.util.findMethods
import com.demonwav.mcdev.util.internalName
import com.intellij.codeInsight.completion.JavaLookupElementBuilder
import com.intellij.extapi.psi.ASTWrapperPsiElement
import com.intellij.lang.ASTNode
import com.intellij.openapi.util.TextRange
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiElementResolveResult
import com.intellij.psi.PsiMethod
import com.intellij.psi.PsiReference
import com.intellij.psi.PsiSubstitutor
import com.intellij.psi.ResolveResult
import com.intellij.psi.util.PsiModificationTracker
import com.intellij.psi.util.PsiTreeUtil
import com.intellij.util.ArrayUtil
import com.intellij.util.IncorrectOperationException

abstract class UnpickMethodNameImplMixin(node: ASTNode) : ASTWrapperPsiElement(node), UnpickMethodNameMixin {

    override val ownerClassName: UnpickClassName?
        get() = PsiTreeUtil.getChildOfType(parent, UnpickClassName::class.java)

    override val descriptor: String?
        get() = parent.node.findChildByType(UnpickTypes.METHOD_DESCRIPTOR)?.text

    private val internalName: String
        get() = text.replace(" ", "").replace("\t", "")

    override fun resolveMethods(): List<PsiMethod> = cached(PsiModificationTracker.MODIFICATION_COUNT) {
        val owner = ownerClassName?.resolveClass() ?: return@cached emptyList()
        owner.findMethods(MemberReference(internalName, descriptor)).toList()
    }

    override fun getElement(): PsiElement = this

    override fun getReference(): PsiReference = this

    override fun multiResolve(incompleteCode: Boolean): Array<ResolveResult> {
        if (isStaticInitializer) {
            // There's not always a PSI element for the static initializer, resolve to the class instead
            val owner = ownerClassName?.resolveClass() ?: return ResolveResult.EMPTY_ARRAY
            return arrayOf(PsiElementResolveResult(owner))
        }
        return PsiElementResolveResult.createResults(resolveMethods())
    }

    override fun resolve(): PsiElement? = multiResolve(false).singleOrNull()?.element

    override fun getVariants(): Array<Any> {
        val owner = ownerClassName?.resolveClass() ?: return ArrayUtil.EMPTY_OBJECT_ARRAY
        return (owner.methods + owner.constructors).map { method ->
            JavaLookupElementBuilder.forMethod(
                method,
                method.internalName,
                PsiSubstitutor.EMPTY,
                null,
            )
        }.toTypedArray()
    }

    override fun getRangeInElement(): TextRange = TextRange(0, textLength)

    override fun getCanonicalText(): String = internalName

    override fun handleElementRename(newElementName: String): PsiElement {
        if (isConstructor || isStaticInitializer) {
            return this
        }
        val identifier = node.findChildByType(UnpickTypes.IDENTIFIER) ?: throw IncorrectOperationException()
        identifier.psi.replace(UnpickElementFactory.createIdentifier(project, newElementName))
        return this
    }

    override fun bindToElement(element: PsiElement): PsiElement {
        throw IncorrectOperationException()
    }

    override fun isReferenceTo(element: PsiElement): Boolean {
        return element is PsiMethod && manager.areElementsEquivalent(element, resolve()) ||
            multiResolve(false).any { manager.areElementsEquivalent(it.element, element) }
    }

    override fun isSoft(): Boolean = false
}
