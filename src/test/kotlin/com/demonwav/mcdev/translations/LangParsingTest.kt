/*
 * Minecraft Dev for IntelliJ
 *
 * https://minecraftdev.org
 *
 * Copyright (c) 2019 minecraft-dev
 *
 * MIT License
 */

package com.demonwav.mcdev.translations

import com.demonwav.mcdev.framework.toSnakeCase
import com.demonwav.mcdev.translations.lang.LangFileType
import com.demonwav.mcdev.translations.lang.LangParserDefinition
import com.intellij.testFramework.ParsingTestCase

class LangParsingTest : ParsingTestCase(
    "com/demonwav/mcdev/lang/parser/fixtures",
    LangFileType.FILE_EXTENSION,
    true,
    LangParserDefinition()
) {
    override fun getTestDataPath() = "src/test/resources"
    override fun getTestName(lowercaseFirstLetter: Boolean) = super.getTestName(lowercaseFirstLetter).toSnakeCase()
    override fun includeRanges() = true

    fun testProperties() = doTest(true)
    fun testComments() = doTest(true)
    fun testMixed() = doTest(true)
}
