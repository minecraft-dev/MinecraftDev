/*
 * Minecraft Dev for IntelliJ
 *
 * https://minecraftdev.org
 *
 * Copyright (c) 2022 minecraft-dev
 *
 * MIT License
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
    override fun getWordsScanner(): WordsScanner? =
        DefaultWordsScanner(
            JsonLexer(),
            TokenSet.create(JsonElementTypes.DOUBLE_QUOTED_STRING, JsonElementTypes.SINGLE_QUOTED_STRING),
            TokenSet.create(JsonElementTypes.BLOCK_COMMENT, JsonElementTypes.LINE_COMMENT),
            TokenSet.EMPTY
        )
}

class LangFindUsagesProvider : TranslationFindUsagesProvider() {
    override fun getWordsScanner(): WordsScanner? =
        DefaultWordsScanner(
            LangLexerAdapter(),
            TokenSet.create(LangTypes.KEY),
            TokenSet.create(LangTypes.COMMENT),
            TokenSet.EMPTY
        )
}
