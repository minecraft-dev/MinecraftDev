/*
 * Minecraft Dev for IntelliJ
 *
 * https://minecraftdev.org
 *
 * Copyright (c) 2019 minecraft-dev
 *
 * MIT License
 */

package com.demonwav.mcdev.i18n.reference

import com.demonwav.mcdev.asset.PlatformAssets
import com.demonwav.mcdev.i18n.I18nConstants
import com.demonwav.mcdev.i18n.index.TranslationIndex
import com.demonwav.mcdev.i18n.lang.MCLangLanguage
import com.demonwav.mcdev.i18n.lang.gen.psi.LangEntry
import com.demonwav.mcdev.i18n.lang.gen.psi.LangTypes
import com.demonwav.mcdev.i18n.translations.TranslationFiles
import com.demonwav.mcdev.util.getSimilarity
import com.demonwav.mcdev.util.mcDomain
import com.intellij.codeInsight.completion.CompletionContributor
import com.intellij.codeInsight.completion.CompletionParameters
import com.intellij.codeInsight.completion.CompletionResultSet
import com.intellij.codeInsight.completion.CompletionType
import com.intellij.codeInsight.completion.CompletionUtil
import com.intellij.codeInsight.completion.PrioritizedLookupElement
import com.intellij.codeInsight.lookup.LookupElementBuilder
import com.intellij.json.JsonLanguage
import com.intellij.json.psi.JsonProperty
import com.intellij.json.psi.JsonStringLiteral
import com.intellij.patterns.PlatformPatterns
import com.intellij.psi.PsiElement
import com.intellij.psi.util.PsiUtilCore

sealed class TranslationCompletionContributor : CompletionContributor() {
    protected fun handleKey(text: String, element: PsiElement, domain: String?, result: CompletionResultSet) {
        if (text.isEmpty()) {
            return
        }

        val defaultEntries = TranslationIndex.getAllDefaultTranslations(element.project, domain)
        val existingKeys = TranslationIndex.getTranslations(element.containingFile ?: return).map { it.key }.toSet()
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
}

class JsonCompletionContributor : TranslationCompletionContributor() {
    override fun fillCompletionVariants(parameters: CompletionParameters, result: CompletionResultSet) {
        if (parameters.completionType != CompletionType.BASIC) {
            return
        }

        val position = parameters.position
        if (!PsiUtilCore.findLanguageFromElement(position).isKindOf(JsonLanguage.INSTANCE)) {
            return
        }

        val file = position.containingFile.originalFile.virtualFile
        if (!TranslationFiles.isTranslationFile(file) || TranslationFiles.getLocale(file) == I18nConstants.DEFAULT_LOCALE) {
            return
        }

        if (KEY_PATTERN.accepts(position)) {
            val text = position.text.let { it.substring(0, it.length - CompletionUtil.DUMMY_IDENTIFIER.length) }
            val domain = file.mcDomain
            handleKey(text, position, domain, result)
        }
    }

    companion object {
        val KEY_PATTERN = PlatformPatterns.psiElement(JsonStringLiteral::class.java).withParent(PlatformPatterns.psiElement(JsonProperty::class.java))
    }
}

class LangCompletionContributor : TranslationCompletionContributor() {
    override fun fillCompletionVariants(parameters: CompletionParameters, result: CompletionResultSet) {
        if (parameters.completionType != CompletionType.BASIC) {
            return
        }

        val position = parameters.position
        if (!PsiUtilCore.findLanguageFromElement(position).isKindOf(MCLangLanguage)) {
            return
        }

        val file = position.containingFile.originalFile.virtualFile
        if (!TranslationFiles.isTranslationFile(file) || TranslationFiles.getLocale(file) == I18nConstants.DEFAULT_LOCALE) {
            return
        }

        if (KEY_PATTERN.accepts(position) || DUMMY_PATTERN.accepts(position)) {
            val text = position.text.let { it.substring(0, it.length - CompletionUtil.DUMMY_IDENTIFIER.length) }
            val domain = file.mcDomain
            handleKey(text, position, domain, result)
        }
    }

    companion object {
        val KEY_PATTERN = PlatformPatterns.psiElement().withElementType(PlatformPatterns.elementType().oneOf(LangTypes.KEY))
        val DUMMY_PATTERN = PlatformPatterns.psiElement().withElementType(PlatformPatterns.elementType().oneOf(LangTypes.DUMMY))
    }
}
