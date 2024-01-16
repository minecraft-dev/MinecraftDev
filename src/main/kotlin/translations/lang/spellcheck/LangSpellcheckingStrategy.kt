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
