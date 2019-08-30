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

import com.demonwav.mcdev.framework.EdtInterceptor
import com.demonwav.mcdev.framework.ProjectBuilder
import com.demonwav.mcdev.framework.ProjectBuilderTest
import com.demonwav.mcdev.framework.testParser
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith

@ExtendWith(EdtInterceptor::class)
@DisplayName("I18n Parsing Tests")
class I18nParsingTest : ProjectBuilderTest() {

    @Test
    @DisplayName("Properties Parsing Test")
    fun propertiesTest() = testParser("parser/fixtures/properties.lang", ProjectBuilder::i18n)

    @Test
    @DisplayName("Comments Parsing Test")
    fun commentsTest() = testParser("parser/fixtures/comments.lang", ProjectBuilder::i18n)

    @Test
    @DisplayName("Mixed Parsing Test")
    fun mixedTest() = testParser("parser/fixtures/mixed.lang", ProjectBuilder::i18n)
}
