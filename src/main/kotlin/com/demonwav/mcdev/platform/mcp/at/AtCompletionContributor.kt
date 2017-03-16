/*
 * Minecraft Dev for IntelliJ
 *
 * https://minecraftdev.org
 *
 * Copyright (c) 2017 minecraft-dev
 *
 * MIT License
 */

package com.demonwav.mcdev.platform.mcp.at

import com.demonwav.mcdev.facet.MinecraftFacet
import com.demonwav.mcdev.platform.mcp.McpModuleType
import com.demonwav.mcdev.platform.mcp.at.completion.AtFieldNameLookupItem
import com.demonwav.mcdev.platform.mcp.at.completion.AtFuncLookupItem
import com.demonwav.mcdev.platform.mcp.at.completion.AtGenericLookupItem
import com.demonwav.mcdev.platform.mcp.at.completion.SrgPrefixMatcher
import com.demonwav.mcdev.platform.mcp.at.gen.psi.AtEntry
import com.demonwav.mcdev.platform.mcp.at.gen.psi.AtFieldName
import com.demonwav.mcdev.platform.mcp.at.gen.psi.AtTypes
import com.demonwav.mcdev.util.fullQualifiedName
import com.demonwav.mcdev.util.nameAndParameterTypes
import com.demonwav.mcdev.util.qualifiedMemberReference
import com.demonwav.mcdev.util.simpleQualifiedMemberReference
import com.intellij.codeInsight.completion.CompletionContributor
import com.intellij.codeInsight.completion.CompletionParameters
import com.intellij.codeInsight.completion.CompletionResultSet
import com.intellij.codeInsight.completion.CompletionType
import com.intellij.openapi.module.ModuleUtilCore
import com.intellij.patterns.PlatformPatterns.elementType
import com.intellij.patterns.PlatformPatterns.psiElement
import com.intellij.patterns.PsiElementPattern
import com.intellij.psi.JavaPsiFacade
import com.intellij.psi.PsiElement
import com.intellij.psi.tree.IElementType
import com.intellij.psi.util.PsiUtilCore
import com.intellij.util.PlatformIcons

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

        if (AFTER_KEYWORD.accepts(parent)) {
            handleAtClassName(parent, result)
        } else if (AFTER_CLASS_NAME.accepts(parent)) {
            handleAtName(parent, result)
        } else if (AFTER_NEWLINE.accepts(parent)) {
            handleNewLine(parent, result)
        }
    }

    private fun handleAtClassName(element: PsiElement, result: CompletionResultSet) {
        val text = element.text.let { it.substring(0, it.length - 19) }
        val currentPackage = text.substringBeforeLast('.')
        val beginning = text.substringAfterLast('.').toLowerCase()

        val module = ModuleUtilCore.findModuleForPsiElement(element) ?: return
        val project = module.project

        val psiPackage = JavaPsiFacade.getInstance(project).findPackage(currentPackage) ?: return
        // TODO: add support for anonymous classes
        psiPackage.classes.asSequence()
            .filter { it.name?.toLowerCase()?.contains(beginning) == true && it.name != "package-info" }
            .distinctBy { it.name }
            .map { AtElementFactory.createClassName(project, it.fullQualifiedName) }
            .map { AtGenericLookupItem(it, PlatformIcons.CLASS_ICON) }
            .onEach { it.priority = 1.0 }
            .forEach(result::addElement)

        psiPackage.subPackages.asSequence()
            .filter { it.name?.toLowerCase()?.contains(beginning) == true }
            .map { AtElementFactory.createClassName(project, it.qualifiedName) }
            .map { AtGenericLookupItem(it, PlatformIcons.PACKAGE_ICON) }
            .onEach { it.priority = 0.0 }
            .forEach(result::addElement)
    }

    private fun handleAtName(memberName: PsiElement, result: CompletionResultSet) {
        if (memberName !is AtFieldName) {
            return
        }

        val text = memberName.text.let { it.substring(0, it.length - 19) }.toLowerCase()

        val entry = memberName.parent as? AtEntry ?: return

        val entryClass = entry.className.classNameValue ?: return

        val module = ModuleUtilCore.findModuleForPsiElement(memberName) ?: return
        val project = module.project

        val mcpModule = MinecraftFacet.getInstance(module)?.getModuleOfType(McpModuleType) ?: return

        val srgMap = mcpModule.srgManager.srgMapNow ?: return

        val kindResult = result.withPrefixMatcher(SrgPrefixMatcher(text))
        entryClass.fields.asSequence()
            .filter { it.name?.toLowerCase()?.contains(text) == true }
            .map { (srgMap.findSrgField(it) ?: it.simpleQualifiedMemberReference) to it.name!! }
            .map { AtElementFactory.createFieldName(project, it.first.text) to it.second }
            .map { AtFieldNameLookupItem(it.first, it.second) }
            .forEach(kindResult::addElement)

        entryClass.methods.asSequence()
            .filter { it.name.toLowerCase().contains(text) }
            .map { (srgMap.findSrgMethod(it) ?: it.qualifiedMemberReference) to it.nameAndParameterTypes }
            .map { AtElementFactory.createFunction(project, it.first.text) to it.second }
            .map { AtFuncLookupItem(it.first, it.second) }
            .forEach(kindResult::addElement)
    }

    fun handleNewLine(element: PsiElement, result: CompletionResultSet) {
        val text = element.text.let { it.substring(0, it.length - 19) }.toLowerCase()

        val project = element.project

        AtElementFactory.Keyword.softMatch(text).asSequence()
            .map { AtElementFactory.createKeyword(project, it) }
            .map { AtGenericLookupItem(it) }
            .forEach(result::addElement)
    }

    companion object {
        fun after(type: IElementType): PsiElementPattern.Capture<PsiElement> =
            psiElement().afterSibling(psiElement().withElementType(elementType().oneOf(type)))

        val AFTER_KEYWORD = after(AtTypes.KEYWORD)
        val AFTER_CLASS_NAME = after(AtTypes.CLASS_NAME)
        val AFTER_NEWLINE = after(AtTypes.CRLF)
    }
}
