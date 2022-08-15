/*
 * Minecraft Dev for IntelliJ
 *
 * https://minecraftdev.org
 *
 * Copyright (c) 2022 minecraft-dev
 *
 * MIT License
 */

package com.demonwav.mcdev.translations.lang.spellcheck

import com.demonwav.mcdev.translations.lang.MCLangLanguage
import com.demonwav.mcdev.translations.lang.gen.psi.LangTypes
import com.intellij.psi.PsiElement
import com.intellij.psi.impl.source.tree.LeafPsiElement
import com.intellij.spellchecker.tokenizer.SpellcheckingStrategy
import com.intellij.spellchecker.tokenizer.Tokenizer
import com.intellij.spellchecker.tokenizer.TokenizerBase

class LangSpellcheckingStrategy : SpellcheckingStrategy() {
    private val langCommentTokenizer = LangCommentTokenizer()
    private val langKeyTokenizer = TokenizerBase<LeafPsiElement>(LangKeySplitter)

    override fun isMyContext(element: PsiElement) = MCLangLanguage.`is`(element.language)

    override fun getTokenizer(element: PsiElement?): Tokenizer<*> {
        return when (element?.node?.elementType) {
            LangTypes.VALUE -> TEXT_TOKENIZER
            LangTypes.COMMENT -> langCommentTokenizer
            LangTypes.EQUALS, LangTypes.LINE_ENDING, LangTypes.ENTRY -> EMPTY_TOKENIZER // Should not be spellchecked
            LangTypes.KEY, LangTypes.DUMMY -> langKeyTokenizer
            else -> super.getTokenizer(element)
        }
    }
}
