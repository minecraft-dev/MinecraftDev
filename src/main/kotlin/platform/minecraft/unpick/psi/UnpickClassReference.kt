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

package com.demonwav.mcdev.platform.minecraft.unpick.psi

import com.demonwav.mcdev.platform.minecraft.unpick.gen.psi.UnpickClassName
import com.demonwav.mcdev.platform.minecraft.unpick.gen.psi.UnpickTypes
import com.demonwav.mcdev.util.findQualifiedClass
import com.intellij.codeInsight.lookup.LookupElementBuilder
import com.intellij.openapi.util.TextRange
import com.intellij.psi.JavaPsiFacade
import com.intellij.psi.PsiClass
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiPackage
import com.intellij.psi.PsiReference
import com.intellij.psi.PsiReferenceBase
import com.intellij.psi.util.ClassUtil
import com.intellij.util.ArrayUtil
import com.intellij.util.IncorrectOperationException

/**
 * A reference from one part of a class or package name to the package or class it names. Class names are binary names,
 * so a single identifier such as `Outer$Inner` contains a separate reference for each class.
 */
class UnpickClassReference(
    element: PsiElement,
    range: TextRange,
    /**
     * The fully qualified name of the referenced package or class, up to and including this part.
     */
    val targetName: String,
    val isPackage: Boolean,
) : PsiReferenceBase<PsiElement>(element, range, false) {

    override fun resolve(): PsiElement? {
        return if (isPackage) {
            JavaPsiFacade.getInstance(element.project).findPackage(targetName)
        } else {
            findQualifiedClass(targetName, element)
        }
    }

    override fun getVariants(): Array<Any> {
        val facade = JavaPsiFacade.getInstance(element.project)
        val scope = element.resolveScope
        val parentName = targetName.substring(0, maxOf(targetName.lastIndexOf('.'), targetName.lastIndexOf('$'), 0))
        val isInnerClass = targetName.lastIndexOf('$') > targetName.lastIndexOf('.')
        if (isInnerClass) {
            val outer = findQualifiedClass(parentName, element) ?: return ArrayUtil.EMPTY_OBJECT_ARRAY
            return outer.innerClasses.mapNotNull { it.name }.map(LookupElementBuilder::create).toTypedArray()
        }
        val parentPackage = facade.findPackage(parentName) ?: return ArrayUtil.EMPTY_OBJECT_ARRAY
        val packages = parentPackage.getSubPackages(scope).mapNotNull { it.name }
            .map { LookupElementBuilder.create(it) }
        val classes = if (isPackage) {
            emptyList()
        } else {
            parentPackage.getClasses(scope).mapNotNull { it.name }.map { LookupElementBuilder.create(it) }
        }
        return (packages + classes).toTypedArray()
    }

    override fun handleElementRename(newElementName: String): PsiElement {
        val identifier = element.node.getChildren(null).firstOrNull {
            it.elementType == UnpickTypes.IDENTIFIER &&
                TextRange.from(it.startOffsetInParent, it.textLength).contains(rangeInElement)
        } ?: throw IncorrectOperationException()
        val identifierRange = rangeInElement.shiftLeft(identifier.startOffsetInParent)
        val newText = identifierRange.replace(identifier.text, newElementName)
        identifier.psi.replace(UnpickElementFactory.createIdentifier(element.project, newText))
        return element
    }

    override fun bindToElement(element: PsiElement): PsiElement {
        val className = this.element as? UnpickClassName
        if (element is PsiClass && className != null && !isPackage) {
            val binaryName = ClassUtil.getBinaryClassName(element) ?: throw IncorrectOperationException()
            return className.replace(UnpickElementFactory.createClassName(element.project, binaryName))
        }
        if (element is PsiPackage && isPackage) {
            return handleElementRename(element.name ?: throw IncorrectOperationException())
        }
        throw IncorrectOperationException()
    }

    companion object {
        fun qualifiedName(element: PsiElement): String {
            return element.node.getChildren(null)
                .filter { it.elementType == UnpickTypes.IDENTIFIER }
                .joinToString(".") { it.text }
        }

        fun create(element: PsiElement, isPackage: Boolean): Array<PsiReference> {
            val identifiers = element.node.getChildren(null).filter { it.elementType == UnpickTypes.IDENTIFIER }
            val result = mutableListOf<PsiReference>()
            val name = StringBuilder()
            for ((index, identifier) in identifiers.withIndex()) {
                val start = identifier.startOffsetInParent
                if (index != 0) {
                    name.append('.')
                }
                if (isPackage || index != identifiers.lastIndex) {
                    name.append(identifier.text)
                    result += UnpickClassReference(
                        element,
                        TextRange(start, start + identifier.textLength),
                        name.toString(),
                        true,
                    )
                } else {
                    // The last part is the class name, which may include inner classes
                    var partStart = 0
                    for ((partIndex, part) in identifier.text.split('$').withIndex()) {
                        if (partIndex != 0) {
                            name.append('$')
                        }
                        name.append(part)
                        result += UnpickClassReference(
                            element,
                            TextRange(start + partStart, start + partStart + part.length),
                            name.toString(),
                            false,
                        )
                        partStart += part.length + 1
                    }
                }
            }
            return result.toTypedArray()
        }
    }
}
