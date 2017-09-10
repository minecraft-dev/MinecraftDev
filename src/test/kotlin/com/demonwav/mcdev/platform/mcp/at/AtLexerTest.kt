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
import com.intellij.openapi.util.io.FileUtil
import com.intellij.openapi.util.text.StringUtil
import com.intellij.testFramework.LexerTestCase
import com.intellij.testFramework.UsefulTestCase
import java.io.File
import java.io.IOException

class AtLexerTest : LexerTestCase() {

    override fun getDirPath() = "src/test/resources/com/demonwav/mcdev/platform/mcp/at/lexer/fixtures"
    override fun createLexer() = AtLexerAdapter()

    override fun getTestName(lowercaseFirstLetter: Boolean) = super.getTestName(lowercaseFirstLetter).substring(1).toSnakeCase("_at")

    // Copied because it wasn't handling the paths correctly
    private fun doTest() {
        val fileName = dirPath + "/" + getTestName(true) + ".cfg"
        var text = ""
        try {
            val fileText = FileUtil.loadFile(File(fileName))
            text = StringUtil.convertLineSeparators(if (shouldTrim()) fileText.trim { it <= ' ' } else fileText)
        } catch (e: IOException) {
            fail("can't load file " + fileName + ": " + e.message)
        }

        val result = printTokens(text, 0, createLexer())

        UsefulTestCase.assertSameLinesWithFile(dirPath + "/" + getTestName(true) + ".txt", result)
    }

    fun `test class names`() = doTest()
    fun `test class values`() = doTest()
    fun `test comments`() = doTest()
    fun `test funcs`() = doTest()
    fun `test keywords`() = doTest()
    fun `test names`() = doTest()
    fun `test primitives`() = doTest()
}
