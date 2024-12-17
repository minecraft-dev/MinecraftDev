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

package com.demonwav.mcdev.platform.mcp.aw

import com.demonwav.mcdev.framework.BaseMinecraftTest
import com.intellij.openapi.command.WriteCommandAction
import com.intellij.psi.codeStyle.CodeStyleManager
import org.intellij.lang.annotations.Language
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

@DisplayName("Access Widener Tests")
class AwFormatterTest : BaseMinecraftTest() {

    private fun doTest(
        @Language("Access Widener") before: String,
        @Language("Access Widener") after: String,
    ) {

        fixture.configureByText(AwFileType, before)
        WriteCommandAction.runWriteCommandAction(fixture.project) {
            CodeStyleManager.getInstance(project).reformat(fixture.file)
        }

        fixture.checkResult(after)
    }

    @Test
    @DisplayName("Entry Comment Spacing")
    fun entryCommentSpacing() {
        doTest("accessible field Test field# A comment", "accessible field Test field # A comment")
    }

    @Test
    @DisplayName("Single Group Alignment")
    fun singleGroupAlignment() {
        doTest(
            """
            accessible field Test field # A comment
            transitive-accessible method AnotherTest method ()V
            """.trimIndent(),
            """
            accessible            field  Test        field # A comment
            transitive-accessible method AnotherTest method ()V
            """.trimIndent()
        )
    }

    @Test
    @DisplayName("Multiple Groups Alignments")
    fun multipleGroupsAlignments() {
        doTest(
            """
            accessWidener v2 named

            accessible field net/minecraft/Group1A field
            transitive-extendable method net/minecraft/Group1BCD method ()V

            accessible field net/minecraft/server/Group2A anotherField
            extendable method net/minecraft/server/Group2BCD someMethod ()V
            # A comment in the middle should not join the two groups
            accessible field net/minecraft/world/Group3A anotherField
            transitive-extendable method net/minecraft/world/Group2BCD someMethod ()V
            """.trimIndent(),
            """
            accessWidener v2 named

            accessible            field  net/minecraft/Group1A   field
            transitive-extendable method net/minecraft/Group1BCD method ()V

            accessible field  net/minecraft/server/Group2A   anotherField
            extendable method net/minecraft/server/Group2BCD someMethod ()V
            # A comment in the middle should not join the two groups
            accessible            field  net/minecraft/world/Group3A   anotherField
            transitive-extendable method net/minecraft/world/Group2BCD someMethod ()V
            """.trimIndent()
        )
    }
}
