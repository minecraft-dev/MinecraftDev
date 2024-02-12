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

package com.demonwav.mcdev.platform.mixin.expression

import com.demonwav.mcdev.platform.mixin.expression.gen.psi.MEExpressionTypes
import com.demonwav.mcdev.platform.mixin.expression.psi.MEExpressionTokenSets
import com.intellij.openapi.editor.DefaultLanguageHighlighterColors
import com.intellij.openapi.editor.HighlighterColors
import com.intellij.openapi.editor.colors.TextAttributesKey
import com.intellij.openapi.editor.colors.TextAttributesKey.createTextAttributesKey
import com.intellij.openapi.fileTypes.SyntaxHighlighterBase
import com.intellij.openapi.fileTypes.SyntaxHighlighterFactory
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.TokenType
import com.intellij.psi.tree.IElementType

class MEExpressionSyntaxHighlighter : SyntaxHighlighterBase() {
    companion object {
        val STRING = createTextAttributesKey(
            "MEEXPRESSION_STRING",
            DefaultLanguageHighlighterColors.STRING
        )
        val STRING_ESCAPE = createTextAttributesKey(
            "MEEXPRESSION_STRING_ESCAPE",
            DefaultLanguageHighlighterColors.VALID_STRING_ESCAPE
        )
        val NUMBER = createTextAttributesKey(
            "MEEXPRESSION_NUMBER",
            DefaultLanguageHighlighterColors.NUMBER
        )
        val KEYWORD = createTextAttributesKey(
            "MEEXPRESSION_KEYWORD",
            DefaultLanguageHighlighterColors.KEYWORD,
        )
        val OPERATOR = createTextAttributesKey(
            "MEEXPRESSION_OPERATOR",
            DefaultLanguageHighlighterColors.OPERATION_SIGN
        )
        val PARENS = createTextAttributesKey(
            "MEEXPRESSION_PARENS",
            DefaultLanguageHighlighterColors.PARENTHESES
        )
        val BRACKETS = createTextAttributesKey(
            "MEEXPRESSION_BRACKETS",
            DefaultLanguageHighlighterColors.BRACKETS
        )
        val BRACES = createTextAttributesKey(
            "MEEXPRESSION_BRACES",
            DefaultLanguageHighlighterColors.BRACES
        )
        val DOT = createTextAttributesKey(
            "MEEXPRESSION_DOT",
            DefaultLanguageHighlighterColors.DOT
        )
        val COMMA = createTextAttributesKey(
            "MEEXPRESSION_COMMA",
            DefaultLanguageHighlighterColors.COMMA
        )
        val CAPTURE = createTextAttributesKey(
            "MEEXPRESSION_CAPTURE",
            DefaultLanguageHighlighterColors.OPERATION_SIGN
        )
        val WILDCARD = createTextAttributesKey(
            "MEEXPRESSION_WILDCARD",
            DefaultLanguageHighlighterColors.OPERATION_SIGN
        )
        val IDENTIFIER = createTextAttributesKey(
            "MEEXPRESSION_IDENTIFIER",
            DefaultLanguageHighlighterColors.IDENTIFIER
        )
        val IDENTIFIER_CALL = createTextAttributesKey(
            "MEEXPRESSION_IDENTIFIER_CALL",
            DefaultLanguageHighlighterColors.FUNCTION_CALL
        )
        val IDENTIFIER_CLASS_NAME = createTextAttributesKey(
            "MEEXPRESSION_IDENTIFIER_CLASS_NAME",
            DefaultLanguageHighlighterColors.CLASS_REFERENCE
        )
        val IDENTIFIER_MEMBER_NAME = createTextAttributesKey(
            "MEEXPRESSION_IDENTIFIER_MEMBER_NAME",
            DefaultLanguageHighlighterColors.INSTANCE_FIELD
        )
        val IDENTIFIER_VARIABLE = createTextAttributesKey(
            "MEEXPRESSION_IDENTIFIER_VARIABLE",
            DefaultLanguageHighlighterColors.LOCAL_VARIABLE
        )
        val IDENTIFIER_TYPE_DECLARATION = createTextAttributesKey(
            "MEEXPRESSION_IDENTIFIER_TYPE_DECLARATION",
            DefaultLanguageHighlighterColors.CLASS_NAME
        )
        val IDENTIFIER_DECLARATION = createTextAttributesKey(
            "MEEXPRESSION_IDENTIFIER_DECLARATION",
            DefaultLanguageHighlighterColors.FUNCTION_DECLARATION
        )
        val BAD_CHAR = createTextAttributesKey(
            "MEEXPRESSION_BAD_CHARACTER",
            HighlighterColors.BAD_CHARACTER
        )

        val STRING_KEYS = arrayOf(STRING)
        val STRING_ESCAPE_KEYS = arrayOf(STRING_ESCAPE)
        val NUMBER_KEYS = arrayOf(NUMBER)
        val KEYWORD_KEYS = arrayOf(KEYWORD)
        val OPERATOR_KEYS = arrayOf(OPERATOR)
        val PARENS_KEYS = arrayOf(PARENS)
        val BRACKETS_KEYS = arrayOf(BRACKETS)
        val BRACES_KEYS = arrayOf(BRACES)
        val DOT_KEYS = arrayOf(DOT)
        val COMMA_KEYS = arrayOf(COMMA)
        val CAPTURE_KEYS = arrayOf(CAPTURE)
        val WILDCARD_KEYS = arrayOf(WILDCARD)
        val IDENTIFIER_KEYS = arrayOf(IDENTIFIER)
        val BAD_CHAR_KEYS = arrayOf(BAD_CHAR)
    }

    override fun getHighlightingLexer() = MEExpressionLexerAdapter()
    override fun getTokenHighlights(tokenType: IElementType): Array<TextAttributesKey> {
        if (tokenType == MEExpressionTypes.TOKEN_STRING_ESCAPE) {
            return STRING_ESCAPE_KEYS
        }
        if (MEExpressionTokenSets.STRINGS.contains(tokenType)) {
            return STRING_KEYS
        }
        if (tokenType == MEExpressionTypes.TOKEN_IDENTIFIER) {
            return IDENTIFIER_KEYS
        }
        if (MEExpressionTokenSets.NUMBERS.contains(tokenType)) {
            return NUMBER_KEYS
        }
        if (MEExpressionTokenSets.KEYWORDS.contains(tokenType)) {
            return KEYWORD_KEYS
        }
        if (MEExpressionTokenSets.OPERATORS.contains(tokenType)) {
            return OPERATOR_KEYS
        }
        if (MEExpressionTokenSets.PARENS.contains(tokenType)) {
            return PARENS_KEYS
        }
        if (MEExpressionTokenSets.BRACKETS.contains(tokenType)) {
            return BRACKETS_KEYS
        }
        if (MEExpressionTokenSets.BRACES.contains(tokenType)) {
            return BRACES_KEYS
        }
        if (tokenType == MEExpressionTypes.TOKEN_DOT) {
            return DOT_KEYS
        }
        if (tokenType == MEExpressionTypes.TOKEN_COMMA) {
            return COMMA_KEYS
        }
        if (tokenType == MEExpressionTypes.TOKEN_AT) {
            return CAPTURE_KEYS
        }
        if (tokenType == MEExpressionTypes.TOKEN_WILDCARD) {
            return WILDCARD_KEYS
        }
        if (tokenType == TokenType.BAD_CHARACTER) {
            return BAD_CHAR_KEYS
        }

        return TextAttributesKey.EMPTY_ARRAY
    }
}

class MEExpressionSyntaxHighlighterFactory : SyntaxHighlighterFactory() {
    override fun getSyntaxHighlighter(project: Project?, virtualFile: VirtualFile?) = MEExpressionSyntaxHighlighter()
}
