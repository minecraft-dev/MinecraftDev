/*
 * Minecraft Dev for IntelliJ
 *
 * https://minecraftdev.org
 *
 * Copyright (c) 2019 minecraft-dev
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
import com.demonwav.mcdev.platform.mcp.at.gen.psi.AtFunction
import com.demonwav.mcdev.platform.mcp.at.gen.psi.AtTypes
import com.demonwav.mcdev.util.anonymousElements
import com.demonwav.mcdev.util.fullQualifiedName
import com.demonwav.mcdev.util.getSimilarity
import com.demonwav.mcdev.util.nameAndParameterTypes
import com.demonwav.mcdev.util.qualifiedMemberReference
import com.demonwav.mcdev.util.simpleQualifiedMemberReference
import com.intellij.codeInsight.completion.CompletionContributor
import com.intellij.codeInsight.completion.CompletionParameters
import com.intellij.codeInsight.completion.CompletionResultSet
import com.intellij.codeInsight.completion.CompletionType
import com.intellij.codeInsight.completion.CompletionUtil
import com.intellij.codeInsight.completion.PrioritizedLookupElement
import com.intellij.codeInsight.lookup.LookupElementBuilder
import com.intellij.openapi.module.ModuleUtilCore
import com.intellij.patterns.PlatformPatterns.elementType
import com.intellij.patterns.PlatformPatterns.psiElement
import com.intellij.patterns.PsiElementPattern
import com.intellij.psi.JavaPsiFacade
import com.intellij.psi.PsiClass
import com.intellij.psi.PsiDocumentManager
import com.intellij.psi.PsiElement
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.psi.search.PsiShortNamesCache
import com.intellij.psi.tree.IElementType
import com.intellij.psi.tree.TokenSet
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

        val parentText = parent.text ?: return
        if (parentText.length < CompletionUtil.DUMMY_IDENTIFIER.length) {
            return
        }
        val text = parentText.substring(0, parentText.length - CompletionUtil.DUMMY_IDENTIFIER.length)

        when {
            AFTER_KEYWORD.accepts(parent) -> handleAtClassName(text, parent, result)
            AFTER_CLASS_NAME.accepts(parent) -> handleAtName(text, parent, result)
            AFTER_NEWLINE.accepts(parent) -> handleNewLine(text, result)
        }
    }

    private fun handleAtClassName(text: String, element: PsiElement, result: CompletionResultSet) {
        if (text.isEmpty()) {
            return
        }

        val currentPackage = text.substringBeforeLast('.', "")
        val beginning = text.substringAfterLast('.', "")

        if (currentPackage == "" || beginning == "") {
            return
        }

        val module = ModuleUtilCore.findModuleForPsiElement(element) ?: return
        val scope = GlobalSearchScope.moduleWithDependenciesAndLibrariesScope(module)
        val project = module.project

        // Short name completion
        if (!text.contains('.')) {
            val kindResult = result.withPrefixMatcher(KindPrefixMatcher(text))
            val cache = PsiShortNamesCache.getInstance(project)

            var counter = 0
            for (className in cache.allClassNames) {
                if (!className.contains(beginning, ignoreCase = true)) {
                    continue
                }

                if (counter++ > 1000) {
                    break // Prevent insane CPU usage
                }

                val classesByName = cache.getClassesByName(className, scope)
                for (classByName in classesByName) {
                    val name = classByName.fullQualifiedName ?: continue
                    kindResult.addElement(
                        PrioritizedLookupElement.withPriority(
                            LookupElementBuilder.create(name).withIcon(PlatformIcons.CLASS_ICON),
                            1.0 + name.getValue(beginning)
                        )
                    )
                }
            }
        }

        // Anonymous and inner class completion
        if (text.contains('$')) {
            val currentClass =
                JavaPsiFacade.getInstance(project).findClass(text.substringBeforeLast('$'), scope) ?: return

            for (innerClass in currentClass.allInnerClasses) {
                if (innerClass.name?.contains(beginning.substringAfterLast('$'), ignoreCase = true) != true) {
                    continue
                }

                val name = innerClass.fullQualifiedName ?: continue
                result.addElement(
                    PrioritizedLookupElement.withPriority(
                        LookupElementBuilder.create(name).withIcon(PlatformIcons.CLASS_ICON),
                        1.0
                    )
                )
            }

            for (anonymousElement in currentClass.anonymousElements) {
                val anonClass = anonymousElement as? PsiClass ?: continue

                val name = anonClass.fullQualifiedName ?: continue
                result.addElement(
                    PrioritizedLookupElement.withPriority(
                        LookupElementBuilder.create(name).withIcon(PlatformIcons.CLASS_ICON),
                        1.0
                    )
                )
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

            if (!psiClass.name!!.contains(beginning, ignoreCase = true) || psiClass.name == "package-info") {
                continue
            }

            if (!used.add(psiClass.name!!)) {
                continue
            }

            val name = psiClass.fullQualifiedName ?: continue
            result.addElement(
                PrioritizedLookupElement.withPriority(
                    LookupElementBuilder.create(name).withIcon(PlatformIcons.CLASS_ICON),
                    1.0
                )
            )
        }
        used.clear() // help GC

        // Packages in package completion
        for (subPackage in psiPackage.subPackages) {
            if (subPackage.name == null) {
                continue
            }

            if (!subPackage.name!!.contains(beginning, ignoreCase = true)) {
                continue
            }

            val name = subPackage.qualifiedName
            result.addElement(
                PrioritizedLookupElement.withPriority(
                    LookupElementBuilder.create(name).withIcon(PlatformIcons.PACKAGE_ICON),
                    0.0
                )
            )
        }
    }

    private fun handleAtName(text: String, memberName: PsiElement, result: CompletionResultSet) {
        if (memberName !is AtFieldName) {
            return
        }

        val entry = memberName.parent as? AtEntry ?: return

        val entryClass = entry.className.classNameValue ?: return

        val module = ModuleUtilCore.findModuleForPsiElement(memberName) ?: return
        val project = module.project

        val mcpModule = MinecraftFacet.getInstance(module)?.getModuleOfType(McpModuleType) ?: return

        val srgMap = mcpModule.srgManager?.srgMapNow ?: return

        val srgResult = result.withPrefixMatcher(SrgPrefixMatcher(text))

        for (field in entryClass.fields) {
            if (!field.name.contains(text, ignoreCase = true)) {
                continue
            }

            val memberReference = srgMap.getSrgField(field) ?: field.simpleQualifiedMemberReference
            srgResult.addElement(
                PrioritizedLookupElement.withPriority(
                    LookupElementBuilder
                        .create(field.name)
                        .withIcon(PlatformIcons.FIELD_ICON)
                        .withTailText(" (${memberReference.name})", true)
                        .withInsertHandler handler@{ context, _ ->
                            val currentElement = context.file.findElementAt(context.startOffset) ?: return@handler
                            currentElement.replace(
                                AtElementFactory.createFieldName(
                                    context.project,
                                    memberReference.name
                                )
                            )

                            // TODO: Fix visibility decrease
                            PsiDocumentManager.getInstance(context.project)
                                .doPostponedOperationsAndUnblockDocument(context.document)
                            val comment = " # ${field.name}"
                            context.document.insertString(context.editor.caretModel.offset, comment)
                            context.editor.caretModel.moveCaretRelatively(comment.length, 0, false, false, false)
                        },
                    1.0
                )
            )
        }

        for (method in entryClass.methods) {
            if (!method.name.contains(text, ignoreCase = true)) {
                continue
            }

            val memberReference = srgMap.getSrgMethod(method) ?: method.qualifiedMemberReference
            srgResult.addElement(
                PrioritizedLookupElement.withPriority(
                    LookupElementBuilder.create(method.nameAndParameterTypes)
                        .withIcon(PlatformIcons.METHOD_ICON)
                        .withTailText(" (${memberReference.name})", true)
                        .withInsertHandler handler@{ context, _ ->
                            var currentElement = context.file.findElementAt(context.startOffset) ?: return@handler
                            var counter = 0
                            while (currentElement !is AtFieldName && currentElement !is AtFunction) {
                                currentElement = currentElement.parent
                                if (counter++ > 3) {
                                    break
                                }
                            }

                            // Hopefully this won't happen lol
                            if (currentElement !is AtFieldName && currentElement !is AtFunction) {
                                return@handler
                            }

                            if (currentElement is AtFieldName) {
                                // get rid of the bad parameters
                                val parent = currentElement.parent
                                val children =
                                    parent.node.getChildren(TokenSet.create(AtTypes.OPEN_PAREN, AtTypes.CLOSE_PAREN))
                                if (children.size == 2) {
                                    parent.node.removeRange(children[0], children[1].treeNext)
                                }
                            }

                            currentElement.replace(
                                AtElementFactory.createFunction(
                                    project,
                                    memberReference.name + memberReference.descriptor
                                )
                            )

                            // TODO: Fix visibility decreases
                            PsiDocumentManager.getInstance(context.project)
                                .doPostponedOperationsAndUnblockDocument(context.document)
                            val comment = " # ${method.name}"
                            context.document.insertString(context.editor.caretModel.offset, comment)
                            context.editor.caretModel.moveCaretRelatively(comment.length, 0, false, false, false)
                        },
                    0.0
                )
            )
        }
    }

    private fun handleNewLine(text: String, result: CompletionResultSet) {
        for (keyword in AtElementFactory.Keyword.softMatch(text)) {
            result.addElement(LookupElementBuilder.create(keyword.text))
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

        return thisName.getSimilarity(text, packageBonus)
    }

    companion object {
        fun after(type: IElementType): PsiElementPattern.Capture<PsiElement> =
            psiElement().afterSibling(psiElement().withElementType(elementType().oneOf(type)))

        val AFTER_KEYWORD = after(AtTypes.KEYWORD)
        val AFTER_CLASS_NAME = after(AtTypes.CLASS_NAME)
        val AFTER_NEWLINE = after(AtTypes.CRLF)
    }
}
