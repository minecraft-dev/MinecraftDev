/*
 * Minecraft Development for IntelliJ
 *
 * https://mcdev.io/
 *
 * Copyright (C) 2023 minecraft-dev
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

import com.intellij.openapi.util.TextRange
import com.intellij.psi.impl.source.tree.LeafPsiElement
import com.intellij.spellchecker.inspections.PlainTextSplitter
import com.intellij.spellchecker.tokenizer.TokenConsumer
import com.intellij.spellchecker.tokenizer.Tokenizer

class LangCommentTokenizer : Tokenizer<LeafPsiElement>() {
    override fun tokenize(element: LeafPsiElement, consumer: TokenConsumer) {
        val text = element.text
        val startOffset = when {
            text.startsWith('#') -> 1
            else -> 0
        }
        val range = TextRange(startOffset, text.length)
        consumer.consumeToken(element, text, false, 0, range, PlainTextSplitter.getInstance())
    }
}
