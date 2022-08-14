/*
 * Minecraft Dev for IntelliJ
 *
 * https://minecraftdev.org
 *
 * Copyright (c) 2022 minecraft-dev
 *
 * MIT License
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
