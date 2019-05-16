/*
 * Minecraft Dev for IntelliJ
 *
 * https://minecraftdev.org
 *
 * Copyright (c) 2018 minecraft-dev
 *
 * MIT License
 */

package com.demonwav.mcdev.i18n.reference

import com.demonwav.mcdev.i18n.I18nConstants
import com.demonwav.mcdev.i18n.lang.LangLexerAdapter
import com.demonwav.mcdev.i18n.lang.gen.psi.LangEntry
import com.demonwav.mcdev.i18n.lang.gen.psi.LangTypes
import com.demonwav.mcdev.i18n.translations.TranslationFiles
import com.intellij.lang.cacheBuilder.DefaultWordsScanner
import com.intellij.lang.cacheBuilder.WordsScanner
import com.intellij.lang.findUsages.FindUsagesProvider
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiNamedElement
import com.intellij.psi.tree.TokenSet

class I18nFindUsagesProvider : FindUsagesProvider {
    override fun getWordsScanner(): WordsScanner? = DefaultWordsScanner(
        LangLexerAdapter(),
        TokenSet.create(LangTypes.KEY),
        TokenSet.create(LangTypes.COMMENT),
        TokenSet.EMPTY
    )

    override fun canFindUsagesFor(psiElement: PsiElement) = psiElement is PsiNamedElement
        && TranslationFiles.getLocale(psiElement.containingFile.virtualFile) == I18nConstants.DEFAULT_LOCALE

    override fun getHelpId(psiElement: PsiElement): String? = null

    override fun getType(element: PsiElement) = if (element is LangEntry) "translation" else ""

    override fun getDescriptiveName(element: PsiElement) = if (element is LangEntry) element.key else ""

    override fun getNodeText(element: PsiElement, useFullName: Boolean) = if (element is LangEntry) "${element.key}=${element.value}" else ""
}
