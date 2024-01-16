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

package com.demonwav.mcdev.translations.lang.colors

import com.demonwav.mcdev.translations.lang.gen.psi.LangTypes
import com.intellij.lexer.Lexer
import com.intellij.openapi.editor.DefaultLanguageHighlighterColors
import com.intellij.openapi.editor.colors.TextAttributesKey
import com.intellij.openapi.fileTypes.SyntaxHighlighterBase
import com.intellij.psi.tree.IElementType

class LangSyntaxHighlighter(private val lexer: Lexer) : SyntaxHighlighterBase() {
    override fun getHighlightingLexer() = lexer

    override fun getTokenHighlights(tokenType: IElementType?) =
        when (tokenType) {
            LangTypes.KEY, LangTypes.DUMMY -> KEY_KEYS
            LangTypes.EQUALS -> EQUALS_KEYS
            LangTypes.VALUE -> VALUE_KEYS
            LangTypes.COMMENT -> COMMENT_KEYS
            else -> EMPTY_KEYS
        }

    companion object {
        val KEY = TextAttributesKey.createTextAttributesKey("I18N_KEY", DefaultLanguageHighlighterColors.KEYWORD)
        val EQUALS =
            TextAttributesKey.createTextAttributesKey("I18N_EQUALS", DefaultLanguageHighlighterColors.OPERATION_SIGN)
        val VALUE = TextAttributesKey.createTextAttributesKey("I18N_VALUE", DefaultLanguageHighlighterColors.STRING)
        val COMMENT =
            TextAttributesKey.createTextAttributesKey("I18N_COMMENT", DefaultLanguageHighlighterColors.LINE_COMMENT)

        val KEY_KEYS = arrayOf(KEY)
        val EQUALS_KEYS = arrayOf(EQUALS)
        val VALUE_KEYS = arrayOf(VALUE)
        val COMMENT_KEYS = arrayOf(COMMENT)
        val EMPTY_KEYS = emptyArray<TextAttributesKey>()
    }
}
