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
import com.demonwav.mcdev.i18n.lang.I18nLexerAdapter
import com.intellij.openapi.util.io.FileUtil
import com.intellij.openapi.util.text.StringUtil
import com.intellij.testFramework.LexerTestCase
import com.intellij.testFramework.UsefulTestCase
import java.io.File
import java.io.IOException

class I18nLexerTest : LexerTestCase() {

    override fun getDirPath() = "src/test/resources/com/demonwav/mcdev/i18n/lexer/fixtures"
    override fun createLexer() = I18nLexerAdapter()

    override fun getTestName(lowercaseFirstLetter: Boolean) = super.getTestName(lowercaseFirstLetter).toSnakeCase()

    // Copied because it wasn't handling the paths correctly
    private fun doTest() {
        val fileName = dirPath + "/" + getTestName(true) + ".${I18nConstants.FILE_EXTENSION}"
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

    fun testComments() = doTest()
    fun testProperties() = doTest()
}
