/*
 * Minecraft Development for IntelliJ
 *
 * https://mcdev.io/
 *
 * Copyright (C) 2026 minecraft-dev
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

package com.demonwav.mcdev.platform.minecraft.unpick

import com.demonwav.mcdev.framework.EdtInterceptor
import com.demonwav.mcdev.framework.ProjectBuilderTest
import org.intellij.lang.annotations.Language
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith

@ExtendWith(EdtInterceptor::class)
@DisplayName("Unpick Annotator Tests")
class UnpickAnnotatorTest : ProjectBuilderTest() {

    private fun doTest(@Language("Unpick") code: String) {
        buildProject {
            unpick("test.unpick", code)
        }
        fixture.checkHighlighting(true, false, false)
    }

    @Test
    @DisplayName("Unsupported Version Is Only A Warning")
    fun unsupportedVersion() = doTest(
        """
        <warning descr="Unpick version 'v2' is not supported, only v3 and v4 files are checked">unpick v2</warning>
        v1 net/minecraft/Foo BAR this is not checked
        """,
    )

    @Test
    @DisplayName("Group Data Types And Formats")
    fun groupDataTypes() = doTest(
        """
        unpick v4
        group <error descr="Groups cannot have data type 'byte'">byte</error>
        group <error descr="Unknown data type 'integer'">integer</error>
        group float f
            @format <error descr="Format 'binary' is not applicable to data type 'float'">binary</error>
        group String s
            @format <error descr="Format 'hex' is not applicable to data type 'String'">hex</error>
            @<error descr="Unknown attribute 'foo'">foo</error>
        """,
    )

    @Test
    @DisplayName("Attributes After Constants")
    fun attributesAfterConstants() = doTest(
        """
        unpick v4
        group int g
            @strict
            1
            <error descr="Attributes must come before all constants in a group">@flags</error>
        """,
    )

    @Test
    @DisplayName("Literals")
    fun literals() = doTest(
        """
        unpick v4
        group int
            -2147483648
            <error descr="Integer out of bounds">2147483648</error>
            0xFFFFFFFF
            <error descr="Illegal escape sequence">'\q'</error>
            <error descr="Malformed number">12abc</error>
        group long
            -9223372036854775808L
            <error descr="Long out of bounds">9223372036854775808L</error>
        """,
    )

    @Test
    @DisplayName("Multiple Wildcards")
    fun multipleWildcards() = doTest(
        """
        unpick v4
        group int
            foo.Bar.* | foo.Baz.<error descr="Only one wildcard field expression is allowed per constant">*</error>
        """,
    )
}
