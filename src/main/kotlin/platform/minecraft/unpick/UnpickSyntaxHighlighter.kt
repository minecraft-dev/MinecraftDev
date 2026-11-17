/*
 * Minecraft Development for IntelliJ
 *
 * https://mcdev.io/
 *
 * Copyright (C) 2026 minecraft-dev
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

package com.demonwav.mcdev.platform.minecraft.unpick

import com.demonwav.mcdev.platform.minecraft.unpick.UnpickParserDefinition.TokenSets
import com.demonwav.mcdev.platform.minecraft.unpick.gen.psi.UnpickTypes
import com.intellij.openapi.editor.DefaultLanguageHighlighterColors
import com.intellij.openapi.editor.HighlighterColors
import com.intellij.openapi.editor.colors.TextAttributesKey
import com.intellij.openapi.fileTypes.SyntaxHighlighterBase
import com.intellij.psi.TokenType
import com.intellij.psi.tree.IElementType

class UnpickSyntaxHighlighter : SyntaxHighlighterBase() {

    override fun getHighlightingLexer() = UnpickLexerAdapter()

    override fun getTokenHighlights(tokenType: IElementType): Array<TextAttributesKey> =
        when (tokenType) {
            in TokenSets.ITEM_KEYWORDS, UnpickTypes.KEYWORD -> KEYWORD_KEYS
            in TokenSets.NUMBERS -> NUMBER_KEYS
            UnpickTypes.STRING, UnpickTypes.CHAR -> STRING_KEYS
            UnpickTypes.METHOD_DESCRIPTOR, UnpickTypes.FIELD_DESCRIPTOR -> DESCRIPTOR_KEYS
            in TokenSets.OPERATORS -> OPERATOR_KEYS
            UnpickTypes.LPAREN, UnpickTypes.RPAREN -> PARENTHESES_KEYS
            UnpickTypes.DOT -> DOT_KEYS
            UnpickTypes.AT -> ATTRIBUTE_KEYS
            UnpickTypes.COMMENT -> COMMENT_KEYS
            UnpickTypes.DOC_COMMENT -> DOC_COMMENT_KEYS
            UnpickTypes.UNSUPPORTED_BODY -> UNSUPPORTED_KEYS
            TokenType.BAD_CHARACTER -> BAD_CHARACTER_KEYS
            else -> EMPTY_KEYS
        }

    companion object {
        val KEYWORD =
            TextAttributesKey.createTextAttributesKey("UNPICK_KEYWORD", DefaultLanguageHighlighterColors.KEYWORD)
        val NUMBER =
            TextAttributesKey.createTextAttributesKey("UNPICK_NUMBER", DefaultLanguageHighlighterColors.NUMBER)
        val STRING =
            TextAttributesKey.createTextAttributesKey("UNPICK_STRING", DefaultLanguageHighlighterColors.STRING)
        val DESCRIPTOR =
            TextAttributesKey.createTextAttributesKey("UNPICK_DESCRIPTOR", DefaultLanguageHighlighterColors.STRING)
        val OPERATOR =
            TextAttributesKey.createTextAttributesKey(
                "UNPICK_OPERATOR",
                DefaultLanguageHighlighterColors.OPERATION_SIGN,
            )
        val PARENTHESES =
            TextAttributesKey.createTextAttributesKey(
                "UNPICK_PARENTHESES",
                DefaultLanguageHighlighterColors.PARENTHESES,
            )
        val DOT =
            TextAttributesKey.createTextAttributesKey("UNPICK_DOT", DefaultLanguageHighlighterColors.DOT)
        val ATTRIBUTE =
            TextAttributesKey.createTextAttributesKey("UNPICK_ATTRIBUTE", DefaultLanguageHighlighterColors.METADATA)
        val COMMENT =
            TextAttributesKey.createTextAttributesKey("UNPICK_COMMENT", DefaultLanguageHighlighterColors.LINE_COMMENT)
        val DOC_COMMENT =
            TextAttributesKey.createTextAttributesKey(
                "UNPICK_DOC_COMMENT",
                DefaultLanguageHighlighterColors.DOC_COMMENT,
            )
        val UNSUPPORTED =
            TextAttributesKey.createTextAttributesKey(
                "UNPICK_UNSUPPORTED",
                DefaultLanguageHighlighterColors.LINE_COMMENT,
            )
        val BAD_CHARACTER =
            TextAttributesKey.createTextAttributesKey("UNPICK_BAD_CHARACTER", HighlighterColors.BAD_CHARACTER)

        // Only applied by the annotator
        val GROUP_NAME =
            TextAttributesKey.createTextAttributesKey("UNPICK_GROUP_NAME", DefaultLanguageHighlighterColors.CONSTANT)
        val CLASS_NAME =
            TextAttributesKey.createTextAttributesKey(
                "UNPICK_CLASS_NAME",
                DefaultLanguageHighlighterColors.CLASS_REFERENCE,
            )
        val FIELD_NAME =
            TextAttributesKey.createTextAttributesKey(
                "UNPICK_FIELD_NAME",
                DefaultLanguageHighlighterColors.STATIC_FIELD,
            )
        val METHOD_NAME =
            TextAttributesKey.createTextAttributesKey(
                "UNPICK_METHOD_NAME",
                DefaultLanguageHighlighterColors.STATIC_METHOD,
            )

        private val KEYWORD_KEYS = arrayOf(KEYWORD)
        private val NUMBER_KEYS = arrayOf(NUMBER)
        private val STRING_KEYS = arrayOf(STRING)
        private val DESCRIPTOR_KEYS = arrayOf(DESCRIPTOR)
        private val OPERATOR_KEYS = arrayOf(OPERATOR)
        private val PARENTHESES_KEYS = arrayOf(PARENTHESES)
        private val DOT_KEYS = arrayOf(DOT)
        private val ATTRIBUTE_KEYS = arrayOf(ATTRIBUTE)
        private val COMMENT_KEYS = arrayOf(COMMENT)
        private val DOC_COMMENT_KEYS = arrayOf(DOC_COMMENT)
        private val UNSUPPORTED_KEYS = arrayOf(UNSUPPORTED)
        private val BAD_CHARACTER_KEYS = arrayOf(BAD_CHARACTER)
        private val EMPTY_KEYS = emptyArray<TextAttributesKey>()
    }
}
