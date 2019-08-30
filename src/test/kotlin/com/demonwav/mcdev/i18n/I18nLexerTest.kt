/*
 * Minecraft Dev for IntelliJ
 *
 * https://minecraftdev.org
 *
 * Copyright (c) 2019 minecraft-dev
 *
 * MIT License
 */

package com.demonwav.mcdev.i18n

import com.demonwav.mcdev.framework.testLexer
import com.demonwav.mcdev.i18n.lang.I18nLexerAdapter
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

@DisplayName("I18n Lexing Tests")
class I18nLexerTest {

    @Test
    @DisplayName("Comments Lexing Test")
    fun commentsTest() = testLexer("lexer/fixtures/comments.lang", I18nLexerAdapter())

    @Test
    @DisplayName("Properties Lexing Test")
    fun propertiesTest() = testLexer("lexer/fixtures/properties.lang", I18nLexerAdapter())
}
