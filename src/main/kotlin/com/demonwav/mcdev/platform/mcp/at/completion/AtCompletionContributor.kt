/*
 * Minecraft Dev for IntelliJ
 *
 * https://minecraftdev.org
 *
 * Copyright (c) 2017 minecraft-dev
 *
 * MIT License
 */

package com.demonwav.mcdev.platform.mcp.at.completion

import com.demonwav.mcdev.facet.MinecraftFacet
import com.demonwav.mcdev.platform.mcp.McpModuleType
import com.demonwav.mcdev.platform.mcp.at.AtElementFactory
import com.demonwav.mcdev.platform.mcp.at.AtLanguage
import com.demonwav.mcdev.platform.mcp.at.gen.psi.AtEntry
import com.demonwav.mcdev.platform.mcp.at.gen.psi.AtFieldName
import com.demonwav.mcdev.platform.mcp.at.gen.psi.AtTypes
import com.demonwav.mcdev.util.anonymousElements
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
import com.intellij.psi.PsiClass
import com.intellij.psi.PsiElement
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.psi.search.PsiShortNamesCache
import com.intellij.psi.tree.IElementType
import com.intellij.psi.util.PsiUtilCore
import com.intellij.util.PlatformIcons

class AtCompletionContributor : CompletionContributor() {

    override fun invokeAutoPopup(position: PsiElement, typeChar: Char): Boolean {
        if (typeChar == '$') {
            return true
        }
        return super.invokeAutoPopup(position, typeChar)
    }

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
        if (text.isEmpty()) {
            return
        }

        val currentPackage = text.substringBeforeLast('.')
        val beginning = text.substringAfterLast('.').toLowerCase()

        val module = ModuleUtilCore.findModuleForPsiElement(element) ?: return
        val scope = GlobalSearchScope.moduleWithDependenciesAndLibrariesScope(module)
        val project = module.project

        // Short name completion
        if (!text.contains('.')) {
            val kindResult = result.withPrefixMatcher(KindPrefixMatcher(text))
            val cache = PsiShortNamesCache.getInstance(project)

            var counter = 0
            for (className in cache.allClassNames) {
                if (!className.toLowerCase().startsWith(beginning)) {
                    continue
                }

                if (counter++ > 1000) {
                    break // Prevent insane CPU usage
                }

                val classesByName = cache.getClassesByName(className, scope)
                for (classByName in classesByName) {
                    val classNameAtElement = AtElementFactory.createClassName(project, classByName.fullQualifiedName)
                    val lookupItem = AtGenericLookupItem(classNameAtElement, PlatformIcons.CLASS_ICON)
                    lookupItem.priority = 1.0 + classNameAtElement.text.getValue(beginning)

                    kindResult.addElement(lookupItem)
                }
            }
        }

        // Anonymous and inner class completion
        if (text.contains('$')) {
            val currentClass = JavaPsiFacade.getInstance(project).findClass(text.substringBeforeLast('$'), scope) ?: return

            for (innerClass in currentClass.allInnerClasses) {
                if (innerClass.name?.toLowerCase()?.contains(beginning.substringAfterLast('$')) != true) {
                    continue
                }

                val classNameAtElement = AtElementFactory.createClassName(project, innerClass.fullQualifiedName)
                val lookupItem = AtGenericLookupItem(classNameAtElement, PlatformIcons.CLASS_ICON)
                lookupItem.priority = 1.0

                result.addElement(lookupItem)
            }

            val anonymousElements = currentClass.anonymousElements ?: arrayOf()
            for (anonymousElement in anonymousElements) {
                val anonClass = anonymousElement as? PsiClass ?: continue

                val classNameAtElement = AtElementFactory.createClassName(project, anonClass.fullQualifiedName)
                val lookupItem = AtGenericLookupItem(classNameAtElement, PlatformIcons.CLASS_ICON)
                lookupItem.priority = 1.0

                result.addElement(lookupItem)
            }
            return
        }

        val psiPackage = JavaPsiFacade.getInstance(project).findPackage(currentPackage) ?: return

        // Classes in package completion
        val used = mutableSetOf<String>()
        for (psiClass in psiPackage.classes) {
            if (psiClass.name == null) {
                continue
            }

            if (!psiClass.name!!.toLowerCase().contains(beginning) || psiClass.name == "package-info") {
                continue
            }

            if (!used.add(psiClass.name!!)) {
                continue
            }

            val classNameAtElement = AtElementFactory.createClassName(project, psiClass.fullQualifiedName)
            val lookupItem = AtGenericLookupItem(classNameAtElement, PlatformIcons.CLASS_ICON)
            lookupItem.priority = 1.0

            result.addElement(lookupItem)
        }
        used.clear() // help GC

        // Packages in package completion
        for (subPackage in psiPackage.subPackages) {
            if (subPackage.name == null) {
                continue
            }

            if (!subPackage.name!!.toLowerCase().contains(beginning)) {
                continue
            }

            val classNameAtElement = AtElementFactory.createClassName(project, subPackage.qualifiedName)
            val lookupItem = AtGenericLookupItem(classNameAtElement, PlatformIcons.PACKAGE_ICON)
            lookupItem.priority = 0.0

            result.addElement(lookupItem)
        }
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

        val srgResult = result.withPrefixMatcher(SrgPrefixMatcher(text))

        for (field in entryClass.fields) {
            if (field.name == null) {
                continue
            }

            if (!field.name!!.toLowerCase().contains(text)) {
                continue
            }

            val memberReference = srgMap.findSrgField(field) ?: field.simpleQualifiedMemberReference
            val fieldName = AtElementFactory.createFieldName(project, memberReference.name)
            val lookupItem = AtFieldNameLookupItem(fieldName, field.name!!)

            srgResult.addElement(lookupItem)
        }

        for (method in entryClass.methods) {
            if (!method.name.toLowerCase().contains(text)) {
                continue
            }

            val memberReference = srgMap.findSrgMethod(method) ?: method.qualifiedMemberReference
            val function = AtElementFactory.createFunction(project, memberReference.name + memberReference.descriptor)
            val lookupItem = AtFuncLookupItem(function, method.nameAndParameterTypes)

            srgResult.addElement(lookupItem)
        }
    }

    fun handleNewLine(element: PsiElement, result: CompletionResultSet) {
        val text = element.text.let { it.substring(0, it.length - 19) }.toLowerCase()

        val project = element.project

        for (keyword in AtElementFactory.Keyword.softMatch(text)) {
            val atKeyword = AtElementFactory.createKeyword(project, keyword)
            val lookupItem = AtGenericLookupItem(atKeyword)
            result.addElement(lookupItem)
        }
    }

    /**
     * This helps order the (hopefully) most relevant entries in the short name completion
     */
    private fun String?.getValue(text: String): Int {
        if (this == null) {
            return 0
        }

        // Push net.minecraft{forge} classes up to the top
        val packageBonus = if (this.startsWith("net.minecraft")) 10_000 else 0

        val thisName = this.substringAfterLast('.')

        if (thisName == text) {
            return 1_000_000 + packageBonus // exact match
        }

        val lowerCaseThis = thisName.toLowerCase()
        val lowerCaseText = text.toLowerCase()

        if (lowerCaseThis == lowerCaseText) {
            return 100_000 + packageBonus // lowercase exact match
        }

        val distance = Math.min(lowerCaseThis.length, lowerCaseText.length)
        for (i in 0 until distance) {
            if (lowerCaseThis[i] != lowerCaseText[i]) {
                return i + packageBonus
            }
        }
        return distance + packageBonus
    }

    companion object {
        fun after(type: IElementType): PsiElementPattern.Capture<PsiElement> =
            psiElement().afterSibling(psiElement().withElementType(elementType().oneOf(type)))

        val AFTER_KEYWORD = after(AtTypes.KEYWORD)
        val AFTER_CLASS_NAME = after(AtTypes.CLASS_NAME)
        val AFTER_NEWLINE = after(AtTypes.CRLF)
    }
}
