/*
 * Minecraft Dev for IntelliJ
 *
 * https://minecraftdev.org
 *
 * Copyright (c) 2021 minecraft-dev
 *
 * MIT License
 */

package com.demonwav.mcdev.platform.mcp.aw.psi.mixins.impl

import com.demonwav.mcdev.platform.mcp.aw.gen.psi.AwFieldEntry
import com.demonwav.mcdev.platform.mcp.aw.gen.psi.AwMethodEntry
import com.demonwav.mcdev.platform.mcp.aw.psi.mixins.AwEntryMixin
import com.demonwav.mcdev.platform.mcp.aw.psi.mixins.AwMemberNameMixin
import com.demonwav.mcdev.util.MemberReference
import com.intellij.extapi.psi.ASTWrapperPsiElement
import com.intellij.lang.ASTNode
import com.intellij.openapi.util.TextRange
import com.intellij.psi.JavaPsiFacade
import com.intellij.psi.PsiClass
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiReference
import com.intellij.psi.util.parentOfType
import com.intellij.util.ArrayUtil
import com.intellij.util.IncorrectOperationException

abstract class AwMemberNameImplMixin(node: ASTNode) : ASTWrapperPsiElement(node), AwMemberNameMixin {

    override fun getElement(): PsiElement = this

    override fun getReference(): PsiReference? = this

    override fun resolve(): PsiElement? {
        val entry = this.parentOfType<AwEntryMixin>() ?: return null
        return when (entry) {
            is AwMethodEntry -> {
                val name = entry.methodName ?: return null
                MemberReference(name, entry.methodDescriptor, entry.targetClassName?.replace('/', '.'))
                    .resolveMember(project, resolveScope)
            }
            is AwFieldEntry -> {
                val name = entry.fieldName ?: return null
                MemberReference(name, null, entry.targetClassName?.replace('/', '.'))
                    .resolveMember(project, resolveScope)
            }
            else -> null
        }
    }

    override fun getVariants(): Array<*> {
        val entry = this.parentOfType<AwEntryMixin>() ?: return ArrayUtil.EMPTY_OBJECT_ARRAY
        val targetClassName = entry.targetClassName ?: return ArrayUtil.EMPTY_OBJECT_ARRAY
        val targetClass = JavaPsiFacade.getInstance(project)?.findClass(targetClassName, resolveScope)
            ?: return ArrayUtil.EMPTY_OBJECT_ARRAY
        return ArrayUtil.mergeArrays(targetClass.methods, targetClass.fields)
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
        return element is PsiClass && element.qualifiedName == text.replace('/', '.')
    }

    override fun isSoft(): Boolean = false
}
