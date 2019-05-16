/*
 * Minecraft Dev for IntelliJ
 *
 * https://minecraftdev.org
 *
 * Copyright (c) 2018 minecraft-dev
 *
 * MIT License
 */

package com.demonwav.mcdev.i18n

import com.demonwav.mcdev.framework.toSnakeCase
import com.demonwav.mcdev.i18n.lang.LangParserDefinition
import com.intellij.testFramework.ParsingTestCase

class I18nParsingTest : ParsingTestCase("com/demonwav/mcdev/i18n/parser/fixtures", I18nConstants.FILE_EXTENSION, true, LangParserDefinition()) {
    override fun getTestDataPath() = "src/test/resources"
    override fun getTestName(lowercaseFirstLetter: Boolean) = super.getTestName(lowercaseFirstLetter).toSnakeCase()
    override fun includeRanges() = true

    fun testProperties() = doTest(true)
    fun testComments() = doTest(true)
    fun testMixed() = doTest(true)
}
