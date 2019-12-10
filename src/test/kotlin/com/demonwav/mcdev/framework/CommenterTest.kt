/*
 * Minecraft Dev for IntelliJ
 *
 * https://minecraftdev.org
 *
 * Copyright (c) 2019 minecraft-dev
 *
 * MIT License
 */

package com.demonwav.mcdev.framework

import com.demonwav.mcdev.platform.PlatformType
import com.intellij.openapi.actionSystem.IdeActions
import com.intellij.openapi.vfs.VfsUtil
import com.intellij.util.io.delete
import java.nio.file.Files
import java.nio.file.Path
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.TestInfo

open class CommenterTest(vararg platformTypes: PlatformType) : BaseMinecraftTest(*platformTypes) {

    private val _testDataPath: Path by lazy {
        Files.createTempDirectory("mcdev")
    }

    override val testDataPath = _testDataPath.toString()

    private lateinit var fileName: String

    protected fun doTest(before: String, after: String, postfix: String, func: ProjectBuilderFunc) {
        buildProject(VfsUtil.findFile(_testDataPath, true)!!) {
            func(fileName + postfix, before, true)
            func("${fileName}_after$postfix", after, false)
        }

        fixture.performEditorAction(IdeActions.ACTION_COMMENT_LINE)
        fixture.checkResultByFile("${fileName}_after$postfix", true)
    }

    @BeforeEach
    fun info(info: TestInfo) {
        fileName = info.displayName
    }

    @AfterEach
    fun cleanup() {
        _testDataPath.delete()
    }
}
