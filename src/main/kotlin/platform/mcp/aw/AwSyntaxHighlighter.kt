/*
 * Minecraft Dev for IntelliJ
 *
 * https://minecraftdev.org
 *
 * Copyright (c) 2022 minecraft-dev
 *
 * MIT License
 */

package com.demonwav.mcdev.platform.mcp.aw

import com.demonwav.mcdev.platform.mcp.aw.gen.psi.AwTypes
import com.intellij.openapi.editor.DefaultLanguageHighlighterColors
import com.intellij.openapi.editor.HighlighterColors
import com.intellij.openapi.editor.colors.TextAttributesKey
import com.intellij.openapi.fileTypes.SyntaxHighlighterBase
import com.intellij.psi.TokenType
import com.intellij.psi.tree.IElementType

class AwSyntaxHighlighter : SyntaxHighlighterBase() {

    override fun getHighlightingLexer() = AwLexerAdapter()

    override fun getTokenHighlights(tokenType: IElementType) =
        when (tokenType) {
            AwTypes.HEADER_NAME -> HEADER_NAME_KEYS
            AwTypes.HEADER_NAMESPACE_ELEMENT -> HEADER_NAMESPACE_KEYS
            AwTypes.ACCESS_ELEMENT -> ACCESS_KEYS
            AwTypes.CLASS_ELEMENT -> CLASS_ELEMENT_KEYS
            AwTypes.METHOD_ELEMENT -> METHOD_ELEMENT_KEYS
            AwTypes.FIELD_ELEMENT -> FIELD_ELEMENT_KEYS
            AwTypes.CLASS_NAME_ELEMENT -> CLASS_NAME_KEYS
            AwTypes.NAME_ELEMENT -> MEMBER_NAME_KEYS
            AwTypes.CLASS_VALUE -> CLASS_VALUE_KEYS
            AwTypes.PRIMITIVE -> PRIMITIVE_KEYS
            AwTypes.COMMENT -> COMMENT_KEYS
            TokenType.BAD_CHARACTER -> BAD_CHARACTER_KEYS
            else -> EMPTY_KEYS
        }

    companion object {
        val HEADER_NAME =
            TextAttributesKey.createTextAttributesKey("AW_HEADER_NAME", DefaultLanguageHighlighterColors.KEYWORD)
        val HEADER_NAMESPACE =
            TextAttributesKey.createTextAttributesKey("AW_HEADER_NAMESPACE", DefaultLanguageHighlighterColors.KEYWORD)
        val ACCESS =
            TextAttributesKey.createTextAttributesKey("AW_ACCESS", DefaultLanguageHighlighterColors.KEYWORD)
        val CLASS_ELEMENT =
            TextAttributesKey.createTextAttributesKey("AW_CLASS_ELEMENT", DefaultLanguageHighlighterColors.KEYWORD)
        val METHOD_ELEMENT =
            TextAttributesKey.createTextAttributesKey("AW_METHOD_ELEMENT", DefaultLanguageHighlighterColors.KEYWORD)
        val FIELD_ELEMENT =
            TextAttributesKey.createTextAttributesKey("AW_FIELD_ELEMENT", DefaultLanguageHighlighterColors.KEYWORD)
        val CLASS_NAME =
            TextAttributesKey.createTextAttributesKey("AW_CLASS_NAME", DefaultLanguageHighlighterColors.STRING)
        val MEMBER_NAME =
            TextAttributesKey.createTextAttributesKey("AW_MEMBER_NAME", DefaultLanguageHighlighterColors.STATIC_FIELD)
        val CLASS_VALUE =
            TextAttributesKey.createTextAttributesKey("AW_CLASS_VALUE", DefaultLanguageHighlighterColors.STATIC_METHOD)
        val PRIMITIVE =
            TextAttributesKey.createTextAttributesKey("AW_PRIMITIVE", DefaultLanguageHighlighterColors.NUMBER)
        val COMMENT =
            TextAttributesKey.createTextAttributesKey("AW_COMMENT", DefaultLanguageHighlighterColors.LINE_COMMENT)
        val BAD_CHARACTER =
            TextAttributesKey.createTextAttributesKey("AW_BAD_CHARACTER", HighlighterColors.BAD_CHARACTER)

        private val HEADER_NAME_KEYS = arrayOf(HEADER_NAME)
        private val HEADER_NAMESPACE_KEYS = arrayOf(HEADER_NAMESPACE)
        private val ACCESS_KEYS = arrayOf(ACCESS)
        private val CLASS_ELEMENT_KEYS = arrayOf(CLASS_ELEMENT)
        private val METHOD_ELEMENT_KEYS = arrayOf(METHOD_ELEMENT)
        private val FIELD_ELEMENT_KEYS = arrayOf(FIELD_ELEMENT)
        private val CLASS_NAME_KEYS = arrayOf(CLASS_NAME)
        private val MEMBER_NAME_KEYS = arrayOf(MEMBER_NAME)
        private val CLASS_VALUE_KEYS = arrayOf(CLASS_VALUE)
        private val PRIMITIVE_KEYS = arrayOf(PRIMITIVE)
        private val COMMENT_KEYS = arrayOf(COMMENT)
        private val BAD_CHARACTER_KEYS = arrayOf(BAD_CHARACTER)
        private val EMPTY_KEYS = emptyArray<TextAttributesKey>()
    }
}
