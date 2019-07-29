/*
 * Minecraft Dev for IntelliJ
 *
 * https://minecraftdev.org
 *
 * Copyright (c) 2019 minecraft-dev
 *
 * MIT License
 */

package com.demonwav.mcdev.i18n.lang

import com.demonwav.mcdev.asset.PlatformAssets
import com.demonwav.mcdev.i18n.I18nConstants
import com.demonwav.mcdev.i18n.Scope
import com.demonwav.mcdev.i18n.findDefaultLangEntries
import com.demonwav.mcdev.i18n.lang.gen.psi.I18nEntry
import com.demonwav.mcdev.i18n.lang.gen.psi.I18nTypes
import com.demonwav.mcdev.util.getSimilarity
import com.demonwav.mcdev.util.mcDomain
import com.intellij.codeInsight.completion.CompletionContributor
import com.intellij.codeInsight.completion.CompletionParameters
import com.intellij.codeInsight.completion.CompletionResultSet
import com.intellij.codeInsight.completion.CompletionType
import com.intellij.codeInsight.completion.CompletionUtil
import com.intellij.codeInsight.completion.PrioritizedLookupElement
import com.intellij.codeInsight.lookup.LookupElementBuilder
import com.intellij.patterns.PlatformPatterns.elementType
import com.intellij.patterns.PlatformPatterns.psiElement
import com.intellij.psi.PsiElement
import com.intellij.psi.util.PsiUtilCore

class I18nCompletionContributor : CompletionContributor() {

    override fun fillCompletionVariants(parameters: CompletionParameters, result: CompletionResultSet) {
        if (parameters.completionType != CompletionType.BASIC) {
            return
        }

        val position = parameters.position
        if (!PsiUtilCore.findLanguageFromElement(position).isKindOf(I18nLanguage)) {
            return
        }

        val file = position.containingFile.originalFile.virtualFile
        if (file.nameWithoutExtension == I18nConstants.DEFAULT_LOCALE) {
            return
        }

        if (KEY_PATTERN.accepts(position) || DUMMY_PATTERN.accepts(position)) {
            val text = position.text.let { it.substring(0, it.length - CompletionUtil.DUMMY_IDENTIFIER.length) }
            val domain = file.mcDomain
            handleKey(text, position, domain, result)
        }
    }

    private fun handleKey(text: String, element: PsiElement, domain: String?, result: CompletionResultSet) {
        if (text.isEmpty()) {
            return
        }

        val defaultEntries = element.project.findDefaultLangEntries(scope = Scope.GLOBAL, domain = domain)
        val existingKeys = element.containingFile.children.mapNotNull { (it as? I18nEntry)?.key }
        val prefixResult = result.withPrefixMatcher(text)

        var counter = 0
        for (entry in defaultEntries) {
            val key = entry.key

            if (!key.contains(text) || existingKeys.contains(key)) {
                continue
            }

            if (counter++ > 1000) {
                break // Prevent insane CPU usage
            }

            prefixResult.addElement(
                PrioritizedLookupElement.withPriority(
                    LookupElementBuilder.create(key).withIcon(PlatformAssets.MINECRAFT_ICON),
                    1.0 + key.getSimilarity(text)
                )
            )
        }
    }

    companion object {
        val KEY_PATTERN = psiElement().withElementType(elementType().oneOf(I18nTypes.KEY))
        val DUMMY_PATTERN = psiElement().withElementType(elementType().oneOf(I18nTypes.DUMMY))
    }
}
