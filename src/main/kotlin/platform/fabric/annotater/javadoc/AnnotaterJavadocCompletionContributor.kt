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

package com.demonwav.mcdev.platform.fabric.annotater.javadoc

import com.intellij.codeInsight.completion.CompletionContributor
import com.intellij.codeInsight.completion.CompletionParameters
import com.intellij.codeInsight.completion.CompletionResultSet
import com.intellij.codeInsight.completion.InsertionContext
import com.intellij.codeInsight.lookup.LookupElement
import com.intellij.codeInsight.lookup.LookupElementDecorator
import com.intellij.psi.PsiMethod
import com.intellij.psi.javadoc.PsiDocComment
import com.intellij.psi.javadoc.PsiMarkdownReferenceLink
import com.intellij.psi.util.PsiTreeUtil
import com.intellij.psi.util.TypeConversionUtil

/**
 * Keeps the parameter types of methods completed in annotater Javadocs fully qualified.
 */
class AnnotaterJavadocCompletionContributor : CompletionContributor() {
    override fun fillCompletionVariants(parameters: CompletionParameters, result: CompletionResultSet) {
        if (AnnotaterJavadocVirtualFile.of(parameters.originalFile) == null) {
            return
        }
        val comment = PsiTreeUtil.getParentOfType(parameters.position, PsiDocComment::class.java) ?: return
        if (!comment.isMarkdownComment) {
            return
        }
        result.runRemainingContributors(parameters) { completion ->
            result.passResult(completion.withLookupElement(QualifiedParametersLookupElement(completion.lookupElement)))
        }
    }

    /**
     * Java's own insertion shortens the parameter types, since it doesn't recognize references in Markdown doc comments
     * as being in a doc comment. They're replaced within the parentheses, so the template Java starts for the
     * parameters keeps working.
     */
    private class QualifiedParametersLookupElement(delegate: LookupElement) :
        LookupElementDecorator<LookupElement>(delegate) {
        override fun handleInsert(context: InsertionContext) {
            super.handleInsert(context)
            val method = psiElement as? PsiMethod ?: return
            val text = context.document.charsSequence
            val open = context.startOffset + method.name.length
            if (!text.startsWith(method.name, context.startOffset) || text.getOrNull(open) != '(') {
                return
            }
            val close = text.indexOf(')', open)
            if (close == -1) {
                return
            }
            context.commitDocument()
            val inLink = PsiTreeUtil.getParentOfType(
                context.file.findElementAt(context.startOffset),
                PsiMarkdownReferenceLink::class.java,
            ) != null
            val parameters = method.parameterList.parameters.joinToString(", ") { parameter ->
                val type = TypeConversionUtil.erasure(parameter.type).canonicalText
                // Brackets would end the link
                if (inLink) type.replace("[", "\\[").replace("]", "\\]") else type
            }
            context.document.replaceString(open + 1, close, parameters)
        }
    }
}
