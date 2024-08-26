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

package com.demonwav.mcdev.platform.mcp.at.completion

import com.demonwav.mcdev.platform.mcp.at.AtElementFactory.Keyword
import com.demonwav.mcdev.platform.mcp.at.AtLanguage
import com.demonwav.mcdev.platform.mcp.at.gen.psi.AtEntry
import com.demonwav.mcdev.platform.mcp.at.gen.psi.AtTypes
import com.demonwav.mcdev.util.fullQualifiedName
import com.intellij.codeInsight.completion.CompletionContributor
import com.intellij.codeInsight.completion.CompletionParameters
import com.intellij.codeInsight.completion.CompletionResultSet
import com.intellij.codeInsight.completion.CompletionType
import com.intellij.codeInsight.completion.JavaLookupElementBuilder
import com.intellij.codeInsight.lookup.LookupElementBuilder
import com.intellij.patterns.PlatformPatterns.psiElement
import com.intellij.patterns.PsiElementPattern
import com.intellij.psi.JavaPsiFacade
import com.intellij.psi.JavaRecursiveElementVisitor
import com.intellij.psi.PsiAnonymousClass
import com.intellij.psi.PsiClass
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiPackage
import com.intellij.psi.TokenType
import com.intellij.psi.tree.IElementType
import com.intellij.psi.util.PsiUtilCore
import com.intellij.psi.util.parentOfType

class AtCompletionContributor : CompletionContributor() {

    override fun fillCompletionVariants(parameters: CompletionParameters, result: CompletionResultSet) {
        if (parameters.completionType != CompletionType.BASIC) {
            return
        }

        val position = parameters.position
        if (!PsiUtilCore.findLanguageFromElement(position).isKindOf(AtLanguage)) {
            return
        }

        val parent = position.parent
        when {
            AFTER_KEYWORD.accepts(parent) -> completeAtClassName(parent, result)
            position.parentOfType<AtEntry>() == null -> completeKeywords(result)
        }
    }

    private fun completeKeywords(result: CompletionResultSet) {
        result.addAllElements(Keyword.entries.map { LookupElementBuilder.create(it.text) })
    }

    private fun completeAtClassName(element: PsiElement, result: CompletionResultSet) {
        if (element.textContains('.')) {
            // Only complete "empty" class names here, the rest is handled by the reference variants
            return
        }

        val mcPackage = JavaPsiFacade.getInstance(element.project).findPackage("net.minecraft") ?: return
        mcPackage.accept(object : JavaRecursiveElementVisitor() {
            override fun visitPackage(aPackage: PsiPackage) {
                aPackage.subPackages.forEach { it.accept(this) }
                aPackage.classes.forEach { it.accept(this); it.acceptChildren(this) }
            }

            override fun visitClass(aClass: PsiClass) {
                if (aClass !is PsiAnonymousClass) {
                    val fqn = aClass.fullQualifiedName
                    if (fqn != null) {
                        result.addElement(JavaLookupElementBuilder.forClass(aClass, fqn))
                    }
                }

                super.visitClass(aClass)
            }
        })
    }

    companion object {
        fun after(type: IElementType): PsiElementPattern.Capture<PsiElement> =
            psiElement().afterSiblingSkipping(psiElement(TokenType.WHITE_SPACE), psiElement(type))

        val AFTER_KEYWORD = after(AtTypes.KEYWORD)
    }
}
