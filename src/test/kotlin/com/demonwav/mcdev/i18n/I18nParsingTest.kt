/*
 * Minecraft Dev for IntelliJ
 *
 * https://minecraftdev.org
 *
 * Copyright (c) 2017 minecraft-dev
 *
 * MIT License
 */

package com.demonwav.mcdev.i18n

import com.demonwav.mcdev.framework.toSnakeCase
import com.demonwav.mcdev.i18n.lang.I18nParserDefintion
import com.intellij.testFramework.ParsingTestCase

class I18nParsingTest : ParsingTestCase("com/demonwav/mcdev/i18n/parser/fixtures", "lang", true, I18nParserDefintion()) {
    override fun getTestDataPath() = "src/test/resources"
    override fun getTestName(lowercaseFirstLetter: Boolean) = super.getTestName(lowercaseFirstLetter).toSnakeCase()

    fun testProperties() = doTest(true)
    fun testComments() = doTest(true)
    fun testMixed() = doTest(true)
}
