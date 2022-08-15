/*
 * Minecraft Dev for IntelliJ
 *
 * https://minecraftdev.org
 *
 * Copyright (c) 2022 minecraft-dev
 *
 * MIT License
 */

package com.demonwav.mcdev.translations

import com.demonwav.mcdev.framework.testLexer
import com.demonwav.mcdev.translations.lang.LangLexerAdapter
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

@DisplayName("MCLang Lexing Tests")
class LangLexerTest {

    @Test
    @DisplayName("Comments Lexing Test")
    fun commentsTest() = testLexer("lexer/fixtures/comments.lang", LangLexerAdapter())

    @Test
    @DisplayName("Properties Lexing Test")
    fun propertiesTest() = testLexer("lexer/fixtures/properties.lang", LangLexerAdapter())
}
