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

import com.demonwav.mcdev.i18n.lang.gen.psi.I18nTypes
import com.demonwav.mcdev.util.mcPath
import com.intellij.json.JsonElementTypes
import com.intellij.json.JsonLexer
import com.intellij.json.JsonTokenType
import com.intellij.json.psi.JsonProperty
import com.intellij.lang.cacheBuilder.DefaultWordsScanner
import com.intellij.lang.cacheBuilder.WordsScanner
import com.intellij.lang.findUsages.FindUsagesProvider
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiNamedElement
import com.intellij.psi.tree.TokenSet

class I18nJsonFindUsagesProvider : FindUsagesProvider {
    override fun getWordsScanner(): WordsScanner? = WORDS_SCANNER

    override fun canFindUsagesFor(psiElement: PsiElement) = psiElement is PsiNamedElement
        && psiElement.containingFile.virtualFile.mcPath?.startsWith("lang/") == true

    override fun getHelpId(psiElement: PsiElement): String? = null

    override fun getType(element: PsiElement) = if (element is JsonProperty) "translation" else ""

    override fun getDescriptiveName(element: PsiElement) = if (element is JsonProperty) element.name else ""

    override fun getNodeText(element: PsiElement, useFullName: Boolean) = if (element is JsonProperty) "${element.name}=${element.value}" else ""

    companion object {
        private val WORDS_SCANNER =
            DefaultWordsScanner(
                JsonLexer(),
                TokenSet.create(JsonElementTypes.DOUBLE_QUOTED_STRING, JsonElementTypes.SINGLE_QUOTED_STRING),
                TokenSet.create(JsonElementTypes.BLOCK_COMMENT, JsonElementTypes.LINE_COMMENT),
                TokenSet.EMPTY
            )
    }
}
