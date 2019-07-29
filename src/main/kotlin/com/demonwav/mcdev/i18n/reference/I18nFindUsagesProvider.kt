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

import com.demonwav.mcdev.i18n.lang.I18nLexerAdapter
import com.demonwav.mcdev.i18n.lang.gen.psi.I18nEntry
import com.demonwav.mcdev.i18n.lang.gen.psi.I18nTypes
import com.intellij.lang.cacheBuilder.DefaultWordsScanner
import com.intellij.lang.cacheBuilder.WordsScanner
import com.intellij.lang.findUsages.FindUsagesProvider
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiNamedElement
import com.intellij.psi.tree.TokenSet

class I18nFindUsagesProvider : FindUsagesProvider {
    override fun getWordsScanner(): WordsScanner? = DefaultWordsScanner(
        I18nLexerAdapter(),
        TokenSet.create(I18nTypes.KEY),
        TokenSet.create(I18nTypes.COMMENT),
        TokenSet.EMPTY
    )

    override fun canFindUsagesFor(psiElement: PsiElement) = psiElement is PsiNamedElement

    override fun getHelpId(psiElement: PsiElement): String? = null

    override fun getType(element: PsiElement) = if (element is I18nEntry) "translation" else ""

    override fun getDescriptiveName(element: PsiElement) = if (element is I18nEntry) element.key else ""

    override fun getNodeText(element: PsiElement, useFullName: Boolean) =
        if (element is I18nEntry) "${element.key}=${element.value}" else ""
}
