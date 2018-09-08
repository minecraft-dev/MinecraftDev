/*
 * Minecraft Dev for IntelliJ
 *
 * https://minecraftdev.org
 *
 * Copyright (c) 2018 minecraft-dev
 *
 * MIT License
 */

package com.demonwav.mcdev.platform.mcp.at

import com.demonwav.mcdev.framework.toSnakeCase
import com.intellij.testFramework.ParsingTestCase

class AtParsingTest : ParsingTestCase("com/demonwav/mcdev/platform/mcp/at/parser/fixtures", "cfg", true, AtParserDefinition()) {

    override fun getTestDataPath() = "src/test/resources"
    override fun getTestName(lowercaseFirstLetter: Boolean) = super.getTestName(lowercaseFirstLetter).substring(1).toSnakeCase("_at")
    override fun includeRanges() = true

    fun `test asterisks`() = doTest(true)
    fun `test comments`() = doTest(true)
    fun `test fields`() = doTest(true)
    fun `test funcs`() = doTest(true)
    fun `test keywords`() = doTest(true)
    fun `test no value`() = doTest(true)
}
