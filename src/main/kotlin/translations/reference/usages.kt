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

import com.demonwav.mcdev.translations.TranslationConstants
import com.demonwav.mcdev.translations.TranslationFiles
import com.demonwav.mcdev.translations.lang.LangLexerAdapter
import com.demonwav.mcdev.translations.lang.gen.psi.LangTypes
import com.intellij.json.JsonElementTypes
import com.intellij.json.JsonLexer
import com.intellij.lang.cacheBuilder.DefaultWordsScanner
import com.intellij.lang.cacheBuilder.WordsScanner
import com.intellij.lang.findUsages.FindUsagesProvider
import com.intellij.psi.PsiElement
import com.intellij.psi.tree.TokenSet

sealed class TranslationFindUsagesProvider : FindUsagesProvider {
    override fun canFindUsagesFor(element: PsiElement) =
        TranslationFiles.toTranslation(element) != null && element.containingFile?.virtualFile.let {
            TranslationFiles.getLocale(it) == TranslationConstants.DEFAULT_LOCALE
        }

    override fun getHelpId(psiElement: PsiElement): String? =
        null

    override fun getType(element: PsiElement) =
        TranslationFiles.toTranslation(element)?.let { "translation" } ?: ""

    override fun getDescriptiveName(element: PsiElement) =
        TranslationFiles.toTranslation(element)?.key ?: ""

    override fun getNodeText(element: PsiElement, useFullName: Boolean) =
        TranslationFiles.toTranslation(element)?.let { "${it.key}=${it.text}" } ?: ""
}

class JsonFindUsagesProvider : TranslationFindUsagesProvider() {
    override fun getWordsScanner(): WordsScanner =
        DefaultWordsScanner(
            JsonLexer(),
            TokenSet.create(JsonElementTypes.DOUBLE_QUOTED_STRING, JsonElementTypes.SINGLE_QUOTED_STRING),
            TokenSet.create(JsonElementTypes.BLOCK_COMMENT, JsonElementTypes.LINE_COMMENT),
            TokenSet.EMPTY,
        )

    override fun equals(other: Any?): Boolean {
        return this === other
    }

    override fun hashCode(): Int {
        return System.identityHashCode(this)
    }
}

class LangFindUsagesProvider : TranslationFindUsagesProvider() {
    override fun getWordsScanner(): WordsScanner =
        DefaultWordsScanner(
            LangLexerAdapter(),
            TokenSet.create(LangTypes.KEY),
            TokenSet.create(LangTypes.COMMENT),
            TokenSet.EMPTY,
        )

    override fun equals(other: Any?): Boolean {
        return this === other
    }

    override fun hashCode(): Int {
        return System.identityHashCode(this)
    }
}
