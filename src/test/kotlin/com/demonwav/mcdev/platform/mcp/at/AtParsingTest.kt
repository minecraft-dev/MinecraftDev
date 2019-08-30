/*
 * Minecraft Dev for IntelliJ
 *
 * https://minecraftdev.org
 *
 * Copyright (c) 2019 minecraft-dev
 *
 * MIT License
 */

package com.demonwav.mcdev.platform.mcp.at

import com.demonwav.mcdev.framework.EdtInterceptor
import com.demonwav.mcdev.framework.ProjectBuilder
import com.demonwav.mcdev.framework.ProjectBuilderTest
import com.demonwav.mcdev.framework.testParser
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith

@ExtendWith(EdtInterceptor::class)
@DisplayName("Access Transformer Parsing Tests")
class AtParsingTest : ProjectBuilderTest() {

    @Test
    @DisplayName("Asterisks Parsing Test")
    fun asterisksTest() = testParser("parser/fixtures/asterisks_at.cfg", ProjectBuilder::at)

    @Test
    @DisplayName("Comments Parsing Test")
    fun commentsTest() = testParser("parser/fixtures/comments_at.cfg", ProjectBuilder::at)

    @Test
    @DisplayName("Fields Parsing Test")
    fun fieldsTest() = testParser("parser/fixtures/fields_at.cfg", ProjectBuilder::at)

    @Test
    @DisplayName("Funcs Parsing Test")
    fun funcsTest() = testParser("parser/fixtures/funcs_at.cfg", ProjectBuilder::at)

    @Test
    @DisplayName("Keywords Parsing Test")
    fun keywordsTest() = testParser("parser/fixtures/keywords_at.cfg", ProjectBuilder::at)

    @Test
    @DisplayName("Empty Value Parsing Test")
    fun noValueTest() = testParser("parser/fixtures/no_value_at.cfg", ProjectBuilder::at)
}
