/*
 * Minecraft Dev for IntelliJ
 *
 * https://minecraftdev.org
 *
 * Copyright (c) 2017 minecraft-dev
 *
 * MIT License
 */

package com.demonwav.mcdev.nbt.lang

import com.demonwav.mcdev.nbt.lang.gen.psi.NbttTypes
import com.intellij.openapi.editor.DefaultLanguageHighlighterColors
import com.intellij.openapi.editor.colors.TextAttributesKey
import com.intellij.openapi.fileTypes.SyntaxHighlighterBase
import com.intellij.psi.tree.IElementType

class NbttSyntaxHighlighter : SyntaxHighlighterBase() {

    override fun getHighlightingLexer() = NbttLexerAdapter()

    override fun getTokenHighlights(tokenType: IElementType): Array<TextAttributesKey> {
        return when (tokenType) {
            NbttTypes.BYTES, NbttTypes.INTS -> KEYWORD_KEYS
            NbttTypes.STRING_LITERAL -> STRING_KEYS
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
        val BYTE = TextAttributesKey.createTextAttributesKey("NBTT_BYTE", DefaultLanguageHighlighterColors.NUMBER)
        val SHORT = TextAttributesKey.createTextAttributesKey("NBTT_SHORT", DefaultLanguageHighlighterColors.NUMBER)
        val INT = TextAttributesKey.createTextAttributesKey("NBTT_INT", DefaultLanguageHighlighterColors.NUMBER)
        val LONG = TextAttributesKey.createTextAttributesKey("NBTT_LONG", DefaultLanguageHighlighterColors.NUMBER)
        val FLOAT = TextAttributesKey.createTextAttributesKey("NBTT_FLOAT", DefaultLanguageHighlighterColors.NUMBER)
        val DOUBLE = TextAttributesKey.createTextAttributesKey("NBTT_DOUBLE", DefaultLanguageHighlighterColors.NUMBER)

        private val KEYWORD_KEYS = arrayOf(KEYWORD)
        private val STRING_KEYS = arrayOf(STRING)
        private val BYTE_KEYS = arrayOf(BYTE)
        private val SHORT_KEYS = arrayOf(SHORT)
        private val INT_KEYS = arrayOf(INT)
        private val LONG_KEYS = arrayOf(LONG)
        private val FLOAT_KEYS = arrayOf(FLOAT)
        private val DOUBLE_KEYS = arrayOf(DOUBLE)
        private val EMPTY_KEYS = emptyArray<TextAttributesKey>()
    }
}
