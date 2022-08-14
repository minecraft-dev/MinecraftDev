/*
 * Minecraft Dev for IntelliJ
 *
 * https://minecraftdev.org
 *
 * Copyright (c) 2022 minecraft-dev
 *
 * MIT License
 */

package com.demonwav.mcdev.framework

import com.intellij.openapi.actionSystem.IdeActions
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.TestInfo

abstract class CommenterTest : BaseMinecraftTest() {

    private lateinit var fileName: String

    protected fun doTest(before: String, after: String, postfix: String, func: ProjectBuilderFunc) {
        buildProject {
            func(fileName + postfix, before.trimIndent(), true, false)
        }

        fixture.performEditorAction(IdeActions.ACTION_COMMENT_LINE)
        fixture.checkResult(after.trimIndent(), true)
    }

    @BeforeEach
    fun info(info: TestInfo) {
        fileName = info.displayName
    }
}
