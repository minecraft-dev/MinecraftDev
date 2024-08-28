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

package com.demonwav.mcdev.platform.mcp.at

import com.demonwav.mcdev.framework.BaseMinecraftTest
import com.intellij.openapi.command.WriteCommandAction
import com.intellij.psi.codeStyle.CodeStyleManager
import org.intellij.lang.annotations.Language
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

@DisplayName("Access Transformer Tests")
class AtFormatterTest : BaseMinecraftTest() {

    private fun doTest(
        @Language("Access Transformers") before: String,
        @Language("Access Transformers") after: String,
    ) {

        fixture.configureByText(AtFileType, before)
        WriteCommandAction.runWriteCommandAction(fixture.project) {
            CodeStyleManager.getInstance(project).reformat(fixture.file)
        }

        fixture.checkResult(after)
    }

    @Test
    @DisplayName("Entry Comment Spacing")
    fun entryCommentSpacing() {
        doTest("public Test field# A comment", "public Test field # A comment")
    }

    @Test
    @DisplayName("Single Group Alignment")
    fun singleGroupAlignment() {
        doTest(
            """
            public Test field # A comment
            public+f AnotherTest method()V
            """.trimIndent(),
            """
            public   Test        field # A comment
            public+f AnotherTest method()V
            """.trimIndent()
        )
    }

    @Test
    @DisplayName("Multiple Groups Alignments")
    fun multipleGroupsAlignments() {
        doTest(
            """
            public net.minecraft.Group1A field
            protected net.minecraft.Group1BCD method()V

            public net.minecraft.server.Group2A anotherField
            public-f net.minecraft.server.Group2BCD someMethod()V
            # A comment in the middle should not join the two groups
            protected net.minecraft.world.Group3A anotherField
            protected-f net.minecraft.world.Group2BCD someMethod()V
            """.trimIndent(),
            """
            public    net.minecraft.Group1A   field
            protected net.minecraft.Group1BCD method()V

            public   net.minecraft.server.Group2A   anotherField
            public-f net.minecraft.server.Group2BCD someMethod()V
            # A comment in the middle should not join the two groups
            protected   net.minecraft.world.Group3A   anotherField
            protected-f net.minecraft.world.Group2BCD someMethod()V
            """.trimIndent()
        )
    }
}
