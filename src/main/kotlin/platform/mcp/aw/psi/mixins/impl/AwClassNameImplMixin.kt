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

import com.demonwav.mcdev.platform.mcp.aw.psi.mixins.AwClassNameMixin
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

abstract class AwClassNameImplMixin(node: ASTNode) : ASTWrapperPsiElement(node), AwClassNameMixin {

    override fun getElement(): PsiElement = this

    override fun getReference(): PsiReference? = this

    override fun resolve(): PsiElement? {
        return cached(PsiModificationTracker.MODIFICATION_COUNT) { findQualifiedClass(text.replace('/', '.'), this) }
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
