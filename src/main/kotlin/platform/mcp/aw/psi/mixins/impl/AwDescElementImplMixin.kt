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

import com.demonwav.mcdev.platform.mcp.aw.psi.mixins.AwDescElementMixin
import com.demonwav.mcdev.util.cached
import com.demonwav.mcdev.util.findQualifiedClass
import com.intellij.extapi.psi.ASTWrapperPsiElement
import com.intellij.lang.ASTNode
import com.intellij.openapi.util.TextRange
import com.intellij.psi.PsiClass
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiReference
import com.intellij.psi.util.PsiModificationTracker
import com.intellij.util.IncorrectOperationException

abstract class AwDescElementImplMixin(node: ASTNode) : ASTWrapperPsiElement(node), AwDescElementMixin {

    override fun getElement(): PsiElement = this

    override fun getReference(): PsiReference? = this

    override fun resolve(): PsiElement? = cached(PsiModificationTracker.MODIFICATION_COUNT) {
        val name = asQualifiedName() ?: return@cached null
        return@cached findQualifiedClass(name, this)
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
        return element is PsiClass && element.qualifiedName == asQualifiedName()
    }

    private fun asQualifiedName(): String? =
        if (text.length > 1)
            text.substring(1, text.length - 1).replace('/', '.')
        else null

    override fun isSoft(): Boolean = false
}
