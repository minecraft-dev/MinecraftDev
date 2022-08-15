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
import com.intellij.spellchecker.inspections.BaseSplitter
import com.intellij.spellchecker.inspections.IdentifierSplitter
import com.intellij.util.Consumer

object LangKeySplitter : BaseSplitter() {
    override fun split(text: String?, range: TextRange, consumer: Consumer<TextRange>) {
        if (text == null) {
            return
        }

        val subText = newBombedCharSequence(text, range)
        val codepoints = subText.split('.')
        val idSplitter = IdentifierSplitter.getInstance()

        var index = range.startOffset
        for (codepoint in codepoints) {
            checkCancelled()
            // Use full text with the correct range
            idSplitter.split(text, TextRange.from(index, codepoint.length), consumer)

            index += codepoint.length + 1 // Add the length of the codepoint plus 1 for the period
        }
    }
}
