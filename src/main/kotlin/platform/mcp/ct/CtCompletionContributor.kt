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

package com.demonwav.mcdev.platform.mcp.ct

import com.demonwav.mcdev.facet.MinecraftFacet
import com.demonwav.mcdev.platform.fabric.FabricModuleType
import com.demonwav.mcdev.platform.fabric.util.FabricConstants
import com.demonwav.mcdev.platform.mcp.ct.gen.psi.CtTypes
import com.demonwav.mcdev.util.findModule
import com.intellij.codeInsight.completion.CodeCompletionHandlerBase
import com.intellij.codeInsight.completion.CompletionContributor
import com.intellij.codeInsight.completion.CompletionParameters
import com.intellij.codeInsight.completion.CompletionProvider
import com.intellij.codeInsight.completion.CompletionResultSet
import com.intellij.codeInsight.completion.CompletionType
import com.intellij.codeInsight.completion.InsertionContext
import com.intellij.codeInsight.completion.PrioritizedLookupElement
import com.intellij.codeInsight.lookup.LookupElementBuilder
import com.intellij.openapi.application.runReadActionBlocking
import com.intellij.patterns.PlatformPatterns
import com.intellij.psi.PsiDocumentManager
import com.intellij.psi.TokenType
import com.intellij.psi.util.elementType
import com.intellij.psi.util.prevLeaf
import com.intellij.util.ProcessingContext

class CtCompletionContributor : CompletionContributor() {
    init {
        extend(null, PlatformPatterns.psiElement(), CtHeaderCompletionProvider)
        val whitespace = PlatformPatterns.psiElement(TokenType.WHITE_SPACE)
        val namespacePattern = PlatformPatterns.psiElement()
            .afterLeafSkipping(whitespace, PlatformPatterns.psiElement(CtTypes.HEADER_VERSION_ELEMENT))
        extend(null, namespacePattern, CtNamespaceCompletionProvider)
        val entryStartPattern = PlatformPatterns.psiElement().afterLeaf(PlatformPatterns.psiElement(CtTypes.CRLF))
        extend(null, entryStartPattern, CtEntryStartCompletionProvider)
        val awTargetPattern = PlatformPatterns.psiElement()
            .afterLeafSkipping(whitespace, PlatformPatterns.psiElement(CtTypes.ACCESS_ELEMENT))
        extend(null, awTargetPattern, CtAwTargetCompletionProvider)
    }
}

private fun insertWhitespace(context: InsertionContext) {
    PsiDocumentManager.getInstance(context.project)
        .doPostponedOperationsAndUnblockDocument(context.document)
    context.document.insertString(context.editor.caretModel.offset, " ")
    context.editor.caretModel.moveCaretRelatively(1, 0, false, false, false)
    context.setLaterRunnable {
        runReadActionBlocking {
            CodeCompletionHandlerBase.createHandler(CompletionType.BASIC)
                .invokeCompletion(context.project, context.editor)
        }
    }
}

private val ADDITIONAL_NAMESPACE_PRIORITY = mapOf(
    "named" to 0.2,
    "official" to 0.1,
)

object CtHeaderCompletionProvider : CompletionProvider<CompletionParameters>() {

    override fun addCompletions(
        parameters: CompletionParameters,
        context: ProcessingContext,
        result: CompletionResultSet,
    ) {
        if (parameters.position.prevLeaf(true) == null) {
            val module = parameters.originalFile.findModule() ?: return
            val fabricModule = MinecraftFacet.getInstance(module, FabricModuleType) ?: return
            for (mappingNamespace in fabricModule.mappingNamespaces) {
                val additionalPriority = ADDITIONAL_NAMESPACE_PRIORITY[mappingNamespace] ?: 0.0
                result.addElement(PrioritizedLookupElement.withPriority(LookupElementBuilder.create("accessWidener v1 $mappingNamespace"), 1.0 + additionalPriority))
                result.addElement(PrioritizedLookupElement.withPriority(LookupElementBuilder.create("accessWidener v2 $mappingNamespace"), 2.0 + additionalPriority))
                for (ctVersion in 1..FabricConstants.CLASS_TWEAKER_VERSION - 2) {
                    result.addElement(PrioritizedLookupElement.withPriority(LookupElementBuilder.create("classTweaker v$ctVersion $mappingNamespace"), 2.0 + ctVersion + additionalPriority))
                }
            }
        }
    }
}

object CtNamespaceCompletionProvider : CompletionProvider<CompletionParameters>() {

    override fun addCompletions(
        parameters: CompletionParameters,
        context: ProcessingContext,
        result: CompletionResultSet,
    ) {
        val module = parameters.originalFile.findModule() ?: return
        val fabricModule = MinecraftFacet.getInstance(module, FabricModuleType) ?: return
        result.addAllElements(fabricModule.mappingNamespaces.map(LookupElementBuilder::create))
    }
}

object CtEntryStartCompletionProvider : CompletionProvider<CompletionParameters>() {

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
            "inject-interface",
            "transitive-inject-interface",
            "extend-enum",
            "transitive-extend-enum",
        ).map { LookupElementBuilder.create(it).withInsertHandler { ctx, _ -> insertWhitespace(ctx) } }
        result.addAllElements(elements)
    }
}

object CtAwTargetCompletionProvider : CompletionProvider<CompletionParameters>() {

    override fun addCompletions(
        parameters: CompletionParameters,
        context: ProcessingContext,
        result: CompletionResultSet,
    ) {
        val text = parameters.position
            .prevLeaf { it.elementType == CtTypes.ACCESS_ELEMENT || it.elementType == CtTypes.CRLF }?.text ?: return
        val elements = CtAnnotator.TokenSets.compatibleByAccessMap.get(text)
            .map { LookupElementBuilder.create(it).withInsertHandler { ctx, _ -> insertWhitespace(ctx) } }
        result.addAllElements(elements)
    }
}
