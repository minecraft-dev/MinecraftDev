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
