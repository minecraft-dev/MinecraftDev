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

package com.demonwav.mcdev.platform.mcp.aw.psi.mixins.impl

import com.demonwav.mcdev.platform.mcp.aw.DeleteEndOfLineInsertionHandler
import com.demonwav.mcdev.platform.mcp.aw.gen.psi.AwEntry
import com.demonwav.mcdev.platform.mcp.aw.gen.psi.AwFieldEntry
import com.demonwav.mcdev.platform.mcp.aw.gen.psi.AwMethodEntry
import com.demonwav.mcdev.platform.mcp.aw.psi.mixins.AwMemberNameMixin
import com.demonwav.mcdev.util.MemberReference
import com.demonwav.mcdev.util.cached
import com.demonwav.mcdev.util.descriptor
import com.intellij.codeInsight.lookup.LookupElementBuilder
import com.intellij.extapi.psi.ASTWrapperPsiElement
import com.intellij.lang.ASTNode
import com.intellij.openapi.util.TextRange
import com.intellij.psi.JavaPsiFacade
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiField
import com.intellij.psi.PsiMember
import com.intellij.psi.PsiMethod
import com.intellij.psi.PsiReference
import com.intellij.psi.util.PsiModificationTracker
import com.intellij.psi.util.parentOfType
import com.intellij.util.ArrayUtil
import com.intellij.util.IncorrectOperationException
import com.intellij.util.PlatformIcons
import com.intellij.util.containers.map2Array

abstract class AwMemberNameImplMixin(node: ASTNode) : ASTWrapperPsiElement(node), AwMemberNameMixin {

    override fun getElement(): PsiElement = this

    override fun getReference(): PsiReference? = this

    override fun resolve(): PsiElement? = cached(PsiModificationTracker.MODIFICATION_COUNT) {
        val entry = this.parentOfType<AwEntry>() ?: return@cached null
        val owner = entry.targetClassName?.replace('/', '.')
        return@cached when (entry) {
            is AwMethodEntry -> {
                val name = entry.methodName ?: return@cached null
                val desc = entry.methodDescriptor
                MemberReference(name, desc, owner).resolveMember(project, resolveScope)
                    // fallback if descriptor is invalid
                    ?: MemberReference(name, null, owner).resolveMember(project, resolveScope)
            }
            is AwFieldEntry -> {
                val name = entry.fieldName ?: return@cached null
                MemberReference(name, null, owner)
                    .resolveMember(project, resolveScope)
            }
            else -> null
        }
    }

    override fun getVariants(): Array<*> {
        val entry = this.parentOfType<AwEntry>() ?: return ArrayUtil.EMPTY_OBJECT_ARRAY
        val targetClassName = entry.targetClassName?.replace('/', '.')?.replace('$', '.')
            ?: return ArrayUtil.EMPTY_OBJECT_ARRAY
        val targetClass = JavaPsiFacade.getInstance(project)?.findClass(targetClassName, resolveScope)
            ?: return ArrayUtil.EMPTY_OBJECT_ARRAY

        return when (entry) {
            is AwMethodEntry -> targetClass.methods.map2Array(::methodLookupElement)
            is AwFieldEntry -> targetClass.fields.map2Array(::fieldLookupElement)
            else -> ArrayUtil.EMPTY_OBJECT_ARRAY
        }
    }

    private fun methodLookupElement(method: PsiMethod): LookupElementBuilder {
        var methodName = if (method.isConstructor) "<init>" else method.name
        return LookupElementBuilder.create("$methodName ${method.descriptor}")
            .withPsiElement(method)
            .withPresentableText(method.name)
            .withTailText("(${method.parameterList.parameters.joinToString(", ") { it.type.presentableText }})", true)
            .withIcon(PlatformIcons.METHOD_ICON)
            .withInsertHandler(DeleteEndOfLineInsertionHandler)
    }

    private fun fieldLookupElement(field: PsiField): LookupElementBuilder {
        return LookupElementBuilder.create("${field.name} ${field.descriptor}")
            .withPsiElement(field)
            .withPresentableText(field.name)
            .withIcon(PlatformIcons.FIELD_ICON)
            .withTypeText(field.type.presentableText, true)
            .withInsertHandler(DeleteEndOfLineInsertionHandler)
    }

    override fun getRangeInElement(): TextRange = TextRange(0, text.length)

    override fun getCanonicalText(): String = text

    override fun handleElementRename(newElementName: String): PsiElement {
        throw IncorrectOperationException()
    }

    override fun bindToElement(element: PsiElement): PsiElement {
        throw IncorrectOperationException()
    }

    override fun isReferenceTo(element: PsiElement): Boolean {
        return when (val memberName = text) {
            "<init>" -> element is PsiMethod && element.isConstructor
            else -> element is PsiMember && element.name == memberName
        }
    }

    override fun isSoft(): Boolean = false
}
