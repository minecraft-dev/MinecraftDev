/*
 * Minecraft Development for IntelliJ
 *
 * https://mcdev.io/
 *
 * Copyright (C) 2024 minecraft-dev
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published
 * by the Free Software Foundation, version 3.0 only.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
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
