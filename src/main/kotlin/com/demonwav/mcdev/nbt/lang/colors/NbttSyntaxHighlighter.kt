/*
 * Minecraft Dev for IntelliJ
 *
 * https://minecraftdev.org
 *
 * Copyright (c) 2017 minecraft-dev
 *
 * MIT License
 */

package com.demonwav.mcdev.nbt.lang.colors

import com.demonwav.mcdev.nbt.lang.NbttLexerAdapter
import com.demonwav.mcdev.nbt.lang.gen.psi.NbttTypes
import com.intellij.openapi.editor.DefaultLanguageHighlighterColors
import com.intellij.openapi.editor.colors.TextAttributesKey
import com.intellij.openapi.fileTypes.SyntaxHighlighterBase
import com.intellij.psi.tree.IElementType

class NbttSyntaxHighlighter : SyntaxHighlighterBase() {

    override fun getHighlightingLexer() = NbttLexerAdapter()

    override fun getTokenHighlights(tokenType: IElementType): Array<TextAttributesKey> {
        return when (tokenType) {
            NbttTypes.BYTES, NbttTypes.INTS, NbttTypes.LONGS -> KEYWORD_KEYS
            NbttTypes.STRING_LITERAL -> STRING_KEYS
            NbttTypes.UNQUOTED_STRING_LITERAL -> UNQUOTED_STRING_KEYS
            NbttTypes.BYTE_LITERAL -> BYTE_KEYS
            NbttTypes.SHORT_LITERAL -> SHORT_KEYS
            NbttTypes.INT_LITERAL -> INT_KEYS
            NbttTypes.LONG_LITERAL -> LONG_KEYS
            NbttTypes.FLOAT_LITERAL -> FLOAT_KEYS
            NbttTypes.DOUBLE_LITERAL -> DOUBLE_KEYS
            else -> EMPTY_KEYS
        }
    }

    companion object {
        val KEYWORD = TextAttributesKey.createTextAttributesKey("NBTT_KEYWORD", DefaultLanguageHighlighterColors.KEYWORD)
        val STRING = TextAttributesKey.createTextAttributesKey("NBTT_STRING", DefaultLanguageHighlighterColors.STRING)
        val UNQUOTED_STRING = TextAttributesKey.createTextAttributesKey("NBTT_UNQUOTED_STRING", STRING)
        val STRING_NAME = TextAttributesKey.createTextAttributesKey("NBTT_STRING_NAME", STRING)
        val UNQUOTED_STRING_NAME = TextAttributesKey.createTextAttributesKey("NBTT_UNQUOTED_STRING_NAME", STRING_NAME)
        val BYTE = TextAttributesKey.createTextAttributesKey("NBTT_BYTE", DefaultLanguageHighlighterColors.NUMBER)
        val SHORT = TextAttributesKey.createTextAttributesKey("NBTT_SHORT", DefaultLanguageHighlighterColors.NUMBER)
        val INT = TextAttributesKey.createTextAttributesKey("NBTT_INT", DefaultLanguageHighlighterColors.NUMBER)
        val LONG = TextAttributesKey.createTextAttributesKey("NBTT_LONG", DefaultLanguageHighlighterColors.NUMBER)
        val FLOAT = TextAttributesKey.createTextAttributesKey("NBTT_FLOAT", DefaultLanguageHighlighterColors.NUMBER)
        val DOUBLE = TextAttributesKey.createTextAttributesKey("NBTT_DOUBLE", DefaultLanguageHighlighterColors.NUMBER)
        val MATERIAL = TextAttributesKey.createTextAttributesKey("NBTT_MATERIAL", STRING)

        val KEYWORD_KEYS = arrayOf(KEYWORD)
        val STRING_KEYS = arrayOf(STRING)
        val UNQUOTED_STRING_KEYS = arrayOf(UNQUOTED_STRING)
        val BYTE_KEYS = arrayOf(BYTE)
        val SHORT_KEYS = arrayOf(SHORT)
        val INT_KEYS = arrayOf(INT)
        val LONG_KEYS = arrayOf(LONG)
        val FLOAT_KEYS = arrayOf(FLOAT)
        val DOUBLE_KEYS = arrayOf(DOUBLE)
        val EMPTY_KEYS = emptyArray<TextAttributesKey>()
    }
}
