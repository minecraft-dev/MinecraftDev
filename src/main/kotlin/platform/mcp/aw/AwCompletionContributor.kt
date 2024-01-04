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

package com.demonwav.mcdev.platform.mcp.aw

import com.demonwav.mcdev.platform.mcp.aw.gen.psi.AwTypes
import com.intellij.codeInsight.completion.CodeCompletionHandlerBase
import com.intellij.codeInsight.completion.CompletionContributor
import com.intellij.codeInsight.completion.CompletionParameters
import com.intellij.codeInsight.completion.CompletionProvider
import com.intellij.codeInsight.completion.CompletionResultSet
import com.intellij.codeInsight.completion.CompletionType
import com.intellij.codeInsight.completion.InsertionContext
import com.intellij.codeInsight.lookup.LookupElementBuilder
import com.intellij.openapi.application.runReadAction
import com.intellij.patterns.PlatformPatterns
import com.intellij.psi.PsiDocumentManager
import com.intellij.psi.TokenType
import com.intellij.psi.util.elementType
import com.intellij.psi.util.prevLeaf
import com.intellij.util.ProcessingContext

class AwCompletionContributor : CompletionContributor() {
    init {
        extend(null, PlatformPatterns.psiElement(), AwHeaderCompletionProvider)
        val whitespace = PlatformPatterns.psiElement(TokenType.WHITE_SPACE)
        val namespacePattern = PlatformPatterns.psiElement()
            .afterLeafSkipping(whitespace, PlatformPatterns.psiElement(AwTypes.HEADER_VERSION_ELEMENT))
        extend(null, namespacePattern, AwNamespaceCompletionProvider)
        val accessPattern = PlatformPatterns.psiElement().afterLeaf(PlatformPatterns.psiElement(AwTypes.CRLF))
        extend(null, accessPattern, AwAccessCompletionProvider)
        val targetPattern = PlatformPatterns.psiElement()
            .afterLeafSkipping(whitespace, PlatformPatterns.psiElement(AwTypes.ACCESS_ELEMENT))
        extend(null, targetPattern, AwTargetCompletionProvider)
    }
}

private fun insertWhitespace(context: InsertionContext) {
    PsiDocumentManager.getInstance(context.project)
        .doPostponedOperationsAndUnblockDocument(context.document)
    context.document.insertString(context.editor.caretModel.offset, " ")
    context.editor.caretModel.moveCaretRelatively(1, 0, false, false, false)
    context.setLaterRunnable {
        runReadAction {
            CodeCompletionHandlerBase.createHandler(CompletionType.BASIC)
                .invokeCompletion(context.project, context.editor)
        }
    }
}

object AwHeaderCompletionProvider : CompletionProvider<CompletionParameters>() {

    override fun addCompletions(
        parameters: CompletionParameters,
        context: ProcessingContext,
        result: CompletionResultSet,
    ) {
        if (parameters.position.prevLeaf(true) == null) {
            result.addElement(LookupElementBuilder.create("accessWidener v1 named"))
            result.addElement(LookupElementBuilder.create("accessWidener v2 named"))
        }
    }
}

object AwNamespaceCompletionProvider : CompletionProvider<CompletionParameters>() {

    override fun addCompletions(
        parameters: CompletionParameters,
        context: ProcessingContext,
        result: CompletionResultSet,
    ) = result.addAllElements(listOf("named", "intermediary").map(LookupElementBuilder::create))
}

object AwAccessCompletionProvider : CompletionProvider<CompletionParameters>() {

    override fun addCompletions(
        parameters: CompletionParameters,
        context: ProcessingContext,
        result: CompletionResultSet,
    ) {
        val elements = listOf(
            "accessible",
            "transitive-accessible",
            "extendable",
            "transitive-extendable",
            "mutable",
            "transitive-mutable",
        ).map { LookupElementBuilder.create(it).withInsertHandler { ctx, _ -> insertWhitespace(ctx) } }
        result.addAllElements(elements)
    }
}

object AwTargetCompletionProvider : CompletionProvider<CompletionParameters>() {

    override fun addCompletions(
        parameters: CompletionParameters,
        context: ProcessingContext,
        result: CompletionResultSet,
    ) {
        val text = parameters.position
            .prevLeaf { it.elementType == AwTypes.ACCESS_ELEMENT || it.elementType == AwTypes.CRLF }?.text
        val elements = AwAnnotator.compatibleByAccessMap.get(text)
            .map { LookupElementBuilder.create(it).withInsertHandler { ctx, _ -> insertWhitespace(ctx) } }
        result.addAllElements(elements)
    }
}
