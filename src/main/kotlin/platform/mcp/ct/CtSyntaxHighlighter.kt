/*
 * Minecraft Development for IntelliJ
 *
 * https://mcdev.io/
 *
 * Copyright (C) 2025 minecraft-dev
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

package com.demonwav.mcdev.platform.mcp.ct

import com.demonwav.mcdev.platform.mcp.ct.gen.psi.CtTypes
import com.intellij.openapi.editor.DefaultLanguageHighlighterColors
import com.intellij.openapi.editor.HighlighterColors
import com.intellij.openapi.editor.colors.TextAttributesKey
import com.intellij.openapi.fileTypes.SyntaxHighlighterBase
import com.intellij.psi.TokenType
import com.intellij.psi.tree.IElementType

class CtSyntaxHighlighter : SyntaxHighlighterBase() {

    override fun getHighlightingLexer() = CtLexerAdapter()

    override fun getTokenHighlights(tokenType: IElementType) =
        when (tokenType) {
            CtTypes.HEADER_NAME -> HEADER_NAME_KEYS
            CtTypes.HEADER_NAMESPACE_ELEMENT -> HEADER_NAMESPACE_KEYS
            CtTypes.ACCESS_ELEMENT -> ACCESS_KEYS
            CtTypes.INJECT_INTERFACE_ELEMENT -> INJECT_INTERFACE_KEYS
            CtTypes.EXTEND_ENUM_ELEMENT -> EXTEND_ENUM_KEYS
            CtTypes.CLASS_ELEMENT -> CLASS_ELEMENT_KEYS
            CtTypes.METHOD_ELEMENT -> METHOD_ELEMENT_KEYS
            CtTypes.FIELD_ELEMENT -> FIELD_ELEMENT_KEYS
            CtTypes.CLASS_NAME_ELEMENT -> CLASS_NAME_KEYS
            CtTypes.NAME_ELEMENT -> MEMBER_NAME_KEYS
            CtTypes.CLASS_VALUE, CtTypes.SIGNATURE_CLASS_VALUE_START, CtTypes.SIGNATURE_CLASS_VALUE_END -> CLASS_VALUE_KEYS
            CtTypes.PRIMITIVE -> PRIMITIVE_KEYS
            CtTypes.TYPE_VARIABLE -> TYPE_VARIABLE_KEYS
            CtTypes.COMMENT -> COMMENT_KEYS
            TokenType.BAD_CHARACTER -> BAD_CHARACTER_KEYS
            else -> EMPTY_KEYS
        }

    companion object {
        val HEADER_NAME =
            TextAttributesKey.createTextAttributesKey("CT_HEADER_NAME", DefaultLanguageHighlighterColors.KEYWORD)
        val HEADER_NAMESPACE =
            TextAttributesKey.createTextAttributesKey("CT_HEADER_NAMESPACE", DefaultLanguageHighlighterColors.CLASS_REFERENCE)
        val ACCESS =
            TextAttributesKey.createTextAttributesKey("CT_ACCESS", DefaultLanguageHighlighterColors.KEYWORD)
        val INJECT_INTERFACE =
            TextAttributesKey.createTextAttributesKey("CT_INJECT_INTERFACE", DefaultLanguageHighlighterColors.KEYWORD)
        val EXTEND_ENUM =
            TextAttributesKey.createTextAttributesKey("CT_EXTEND_ENUM", DefaultLanguageHighlighterColors.KEYWORD)
        val CLASS_ELEMENT =
            TextAttributesKey.createTextAttributesKey("CT_CLASS_ELEMENT", DefaultLanguageHighlighterColors.KEYWORD)
        val METHOD_ELEMENT =
            TextAttributesKey.createTextAttributesKey("CT_METHOD_ELEMENT", DefaultLanguageHighlighterColors.KEYWORD)
        val FIELD_ELEMENT =
            TextAttributesKey.createTextAttributesKey("CT_FIELD_ELEMENT", DefaultLanguageHighlighterColors.KEYWORD)
        val CLASS_NAME =
            TextAttributesKey.createTextAttributesKey("CT_CLASS_NAME", DefaultLanguageHighlighterColors.STRING)
        val MEMBER_NAME =
            TextAttributesKey.createTextAttributesKey("CT_MEMBER_NAME", DefaultLanguageHighlighterColors.STATIC_FIELD)
        val CLASS_VALUE =
            TextAttributesKey.createTextAttributesKey("CT_CLASS_VALUE", DefaultLanguageHighlighterColors.STATIC_METHOD)
        val PRIMITIVE =
            TextAttributesKey.createTextAttributesKey("CT_PRIMITIVE", DefaultLanguageHighlighterColors.NUMBER)
        val TYPE_VARIABLE =
            TextAttributesKey.createTextAttributesKey("CT_TYPE_VARIABLE", DefaultLanguageHighlighterColors.STATIC_METHOD)
        val COMMENT =
            TextAttributesKey.createTextAttributesKey("CT_COMMENT", DefaultLanguageHighlighterColors.LINE_COMMENT)
        val BAD_CHARACTER =
            TextAttributesKey.createTextAttributesKey("CT_BAD_CHARACTER", HighlighterColors.BAD_CHARACTER)

        private val HEADER_NAME_KEYS = arrayOf(HEADER_NAME)
        private val HEADER_NAMESPACE_KEYS = arrayOf(HEADER_NAMESPACE)
        private val ACCESS_KEYS = arrayOf(ACCESS)
        private val INJECT_INTERFACE_KEYS = arrayOf(INJECT_INTERFACE)
        private val EXTEND_ENUM_KEYS = arrayOf(EXTEND_ENUM)
        private val CLASS_ELEMENT_KEYS = arrayOf(CLASS_ELEMENT)
        private val METHOD_ELEMENT_KEYS = arrayOf(METHOD_ELEMENT)
        private val FIELD_ELEMENT_KEYS = arrayOf(FIELD_ELEMENT)
        private val CLASS_NAME_KEYS = arrayOf(CLASS_NAME)
        private val MEMBER_NAME_KEYS = arrayOf(MEMBER_NAME)
        private val CLASS_VALUE_KEYS = arrayOf(CLASS_VALUE)
        private val PRIMITIVE_KEYS = arrayOf(PRIMITIVE)
        private val TYPE_VARIABLE_KEYS = arrayOf(TYPE_VARIABLE)
        private val COMMENT_KEYS = arrayOf(COMMENT)
        private val BAD_CHARACTER_KEYS = arrayOf(BAD_CHARACTER)
        private val EMPTY_KEYS = emptyArray<TextAttributesKey>()
    }
}
