/*
 * Minecraft Dev for IntelliJ
 *
 * https://minecraftdev.org
 *
 * Copyright (c) 2022 minecraft-dev
 *
 * MIT License
 */

package com.demonwav.mcdev.platform.mcp.aw.psi.mixins.impl

import com.demonwav.mcdev.platform.mcp.aw.gen.psi.AwFieldEntry
import com.demonwav.mcdev.platform.mcp.aw.gen.psi.AwMethodEntry
import com.demonwav.mcdev.platform.mcp.aw.psi.mixins.AwEntryMixin
import com.demonwav.mcdev.platform.mcp.aw.psi.mixins.AwMemberNameMixin
import com.demonwav.mcdev.util.MemberReference
import com.demonwav.mcdev.util.cached
import com.intellij.codeInsight.completion.JavaLookupElementBuilder
import com.intellij.extapi.psi.ASTWrapperPsiElement
import com.intellij.lang.ASTNode
import com.intellij.openapi.util.TextRange
import com.intellij.psi.JavaPsiFacade
import com.intellij.psi.PsiClass
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiMethod
import com.intellij.psi.PsiReference
import com.intellij.psi.PsiSubstitutor
import com.intellij.psi.util.PsiModificationTracker
import com.intellij.psi.util.parentOfType
import com.intellij.util.ArrayUtil
import com.intellij.util.IncorrectOperationException
import com.intellij.util.containers.map2Array

abstract class AwMemberNameImplMixin(node: ASTNode) : ASTWrapperPsiElement(node), AwMemberNameMixin {

    override fun getElement(): PsiElement = this

    override fun getReference(): PsiReference? = this

    override fun resolve(): PsiElement? = cached(PsiModificationTracker.MODIFICATION_COUNT) {
        val entry = this.parentOfType<AwEntryMixin>() ?: return@cached null
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
        val entry = this.parentOfType<AwEntryMixin>() ?: return ArrayUtil.EMPTY_OBJECT_ARRAY
        val targetClassName = entry.targetClassName?.replace('/', '.')?.replace('$', '.')
            ?: return ArrayUtil.EMPTY_OBJECT_ARRAY
        val targetClass = JavaPsiFacade.getInstance(project)?.findClass(targetClassName, resolveScope)
            ?: return ArrayUtil.EMPTY_OBJECT_ARRAY

        return when (entry) {
            is AwMethodEntry -> targetClass.methods.map2Array(::methodLookupElement)
            is AwFieldEntry -> targetClass.fields
            else -> ArrayUtil.EMPTY_OBJECT_ARRAY
        }
    }

    private fun methodLookupElement(it: PsiMethod) =
        JavaLookupElementBuilder.forMethod(it, if (it.isConstructor) "<init>" else it.name, PsiSubstitutor.EMPTY, null)

    override fun getRangeInElement(): TextRange = TextRange(0, text.length)

    override fun getCanonicalText(): String = text

    override fun handleElementRename(newElementName: String): PsiElement {
        throw IncorrectOperationException()
    }

    override fun bindToElement(element: PsiElement): PsiElement {
        throw IncorrectOperationException()
    }

    override fun isReferenceTo(element: PsiElement): Boolean {
        return element is PsiClass && element.qualifiedName == text.replace('/', '.')
    }

    override fun isSoft(): Boolean = false
}
