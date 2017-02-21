/*
 * Minecraft Dev for IntelliJ
 *
 * https://minecraftdev.org
 *
 * Copyright (c) 2017 minecraft-dev
 *
 * MIT License
 */

package com.demonwav.mcdev.platform.mcp.at

import com.demonwav.mcdev.framework.toSnakeCase
import com.intellij.testFramework.ParsingTestCase

class AtParsingTest : ParsingTestCase("com/demonwav/mcdev/platform/mcp/at/parser/fixtures", "cfg", true, AtParserDefinition()) {

    override fun getTestDataPath() = "src/test/resources"
    override fun getTestName(lowercaseFirstLetter: Boolean) = super.getTestName(lowercaseFirstLetter).toSnakeCase("_at")

    fun testComments() = doTest(true)
    fun testKeywords() = doTest(true)
    fun testFields() = doTest(true)
    fun testFuncs() = doTest(true)
    fun testNoValue() = doTest(true)
    fun testAsterisks() = doTest(true)
}
