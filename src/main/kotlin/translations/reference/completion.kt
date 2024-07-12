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

package com.demonwav.mcdev.translations.reference

import com.demonwav.mcdev.asset.PlatformAssets
import com.demonwav.mcdev.translations.TranslationConstants
import com.demonwav.mcdev.translations.TranslationFiles
import com.demonwav.mcdev.translations.index.TranslationIndex
import com.demonwav.mcdev.translations.lang.MCLangLanguage
import com.demonwav.mcdev.translations.lang.gen.psi.LangTypes
import com.demonwav.mcdev.util.getSimilarity
import com.demonwav.mcdev.util.mcDomain
import com.intellij.codeInsight.completion.CompletionContributor
import com.intellij.codeInsight.completion.CompletionParameters
import com.intellij.codeInsight.completion.CompletionResultSet
import com.intellij.codeInsight.completion.CompletionType
import com.intellij.codeInsight.completion.CompletionUtil
import com.intellij.codeInsight.completion.PrioritizedLookupElement
import com.intellij.codeInsight.lookup.LookupElementBuilder
import com.intellij.json.JsonElementTypes
import com.intellij.json.JsonLanguage
import com.intellij.json.psi.JsonStringLiteral
import com.intellij.openapi.util.text.StringUtil
import com.intellij.patterns.PlatformPatterns
import com.intellij.psi.PsiElement
import com.intellij.psi.util.PsiUtilCore

sealed class TranslationCompletionContributor : CompletionContributor() {
    protected fun handleKey(text: String, element: PsiElement, domain: String?, result: CompletionResultSet) {
        val defaultEntries = TranslationIndex.getAllDefaultTranslations(element.project, domain)
        val availableKeys = TranslationIndex.getTranslations(element.containingFile.originalFile).map { it.key }.toSet()
        val prefixResult = result.withPrefixMatcher(text)

        for (entry in defaultEntries) {
            val key = entry.key

            if (!key.contains(text) || availableKeys.contains(key)) {
                continue
            }

            val textHint = StringUtil.shortenTextWithEllipsis(entry.text, 30, 0)
            prefixResult.addElement(
                PrioritizedLookupElement.withPriority(
                    LookupElementBuilder.create(key).withIcon(PlatformAssets.MINECRAFT_ICON).withTypeText(textHint),
                    1.0 + key.getSimilarity(text),
                ),
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
        if (
            !TranslationFiles.isTranslationFile(file) ||
            TranslationFiles.getLocale(file) == TranslationConstants.DEFAULT_LOCALE
        ) {
            return
        }

        val text = getKey(position)
        if (text != null) {
            val domain = file.mcDomain
            handleKey(text.substring(0, text.length - CompletionUtil.DUMMY_IDENTIFIER.length), position, domain, result)
        }
    }

    private tailrec fun getKey(element: PsiElement): String? {
        if (element.node.elementType == JsonElementTypes.DOUBLE_QUOTED_STRING) {
            return getKey(element.parent)
        }
        if (element is JsonStringLiteral && element.isPropertyName) {
            return element.value
        }
        return null
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
        if (
            !TranslationFiles.isTranslationFile(file) ||
            TranslationFiles.getLocale(file) == TranslationConstants.DEFAULT_LOCALE
        ) {
            return
        }

        if (KEY_PATTERN.accepts(position) || DUMMY_PATTERN.accepts(position)) {
            val text = position.text.let { it.substring(0, it.length - CompletionUtil.DUMMY_IDENTIFIER.length) }
            val domain = file.mcDomain
            handleKey(text, position, domain, result)
        }
    }

    companion object {
        val KEY_PATTERN = PlatformPatterns.psiElement()
            .withElementType(PlatformPatterns.elementType().oneOf(LangTypes.KEY))
        val DUMMY_PATTERN = PlatformPatterns.psiElement()
            .withElementType(PlatformPatterns.elementType().oneOf(LangTypes.DUMMY))
    }
}
