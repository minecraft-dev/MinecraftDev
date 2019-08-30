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

import com.demonwav.mcdev.framework.BaseMinecraftTest
import com.demonwav.mcdev.framework.EdtInterceptor
import com.demonwav.mcdev.platform.PlatformType
import com.intellij.openapi.actionSystem.IdeActions
import com.intellij.openapi.vfs.VfsUtil
import com.intellij.util.io.delete
import org.intellij.lang.annotations.Language
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInfo
import org.junit.jupiter.api.extension.ExtendWith
import java.nio.file.Files
import java.nio.file.Path

@ExtendWith(EdtInterceptor::class)
@DisplayName("I18n Commenter Tests")
class I18nCommenterTest : BaseMinecraftTest(PlatformType.MCP) {

    private val _testDataPath: Path by lazy {
        Files.createTempDirectory("mcdev")
    }

    override val testDataPath = _testDataPath.toString()

    private lateinit var fileName: String

    private fun doTest(@Language("I18n") before: String, @Language("I18n") after: String) {
        buildProject(VfsUtil.findFile(_testDataPath, true)!!) {
            i18n("$fileName.lang", before, configure = true)
            i18n("${fileName}_after.lang", after, configure = false)
        }

        fixture.performEditorAction(IdeActions.ACTION_COMMENT_LINE)
        fixture.checkResultByFile("${fileName}_after.lang", true)
    }

    @BeforeEach
    fun info(info: TestInfo) {
        fileName = info.displayName
    }

    @AfterEach
    fun cleanup() {
        _testDataPath.delete()
    }

    @Test
    @DisplayName("Single Line Comment Test")
    fun singleLineCommentTest() = doTest("""
        test.<caret>key1=value1
        test.key2=value2
    """, """
        #test.key1=value1
        test.k<caret>ey2=value2
    """)

    @Test
    @DisplayName("Multi Line Comment Test")
    fun multiLineCommentTest() = doTest("""
        test.key1=value1
        test.<selection>key2=value2
        test</selection>.key3=value3
        test.key4=value4
    """, """
        test.key1=value1
        #test.<selection>key2=value2
        #test</selection>.key3=value3
        test.key4=value4
    """)

    @Test
    @DisplayName("Single Line Uncomment Test")
    fun singleLineUncommentTest() = doTest("""
        test.key1=value1
        test.key2=value2
        #test<caret>.key3=value3
        #test.key4=value4
    """, """
        test.key1=value1
        test.key2=value2
        test.key3=value3
        #tes<caret>t.key4=value4
    """)

    @Test
    @DisplayName("Multi Line Uncomment Test")
    fun multiLineUncommentTest() = doTest("""
        #test.<selection>key1=value1
        #test.key2=</selection>value2
        #test.key3=value3
        test.key4=value4
    """, """
        test.<selection>key1=value1
        test.key2=</selection>value2
        #test.key3=value3
        test.key4=value4
    """)
}
