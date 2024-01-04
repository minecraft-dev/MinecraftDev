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
