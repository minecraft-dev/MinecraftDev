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

import com.demonwav.mcdev.framework.EdtInterceptor
import com.demonwav.mcdev.framework.ProjectBuilder
import com.demonwav.mcdev.framework.ProjectBuilderTest
import com.demonwav.mcdev.framework.testParser
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith

@ExtendWith(EdtInterceptor::class)
@DisplayName("MCLang Parsing Tests")
class LangParsingTest : ProjectBuilderTest() {

    @Test
    @DisplayName("Properties Parsing Test")
    fun propertiesTest() = testParser("parser/fixtures/properties.lang", ProjectBuilder::lang)

    @Test
    @DisplayName("Comments Parsing Test")
    fun commentsTest() = testParser("parser/fixtures/comments.lang", ProjectBuilder::lang)

    @Test
    @DisplayName("Mixed Parsing Test")
    fun mixedTest() = testParser("parser/fixtures/mixed.lang", ProjectBuilder::lang)
}
