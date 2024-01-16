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

package com.demonwav.mcdev.translations

import com.demonwav.mcdev.framework.CommenterTest
import com.demonwav.mcdev.framework.EdtInterceptor
import com.demonwav.mcdev.framework.ProjectBuilder
import org.intellij.lang.annotations.Language
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith

@ExtendWith(EdtInterceptor::class)
@DisplayName("Minecraft Lang Commenter Tests")
class LangCommenterTest : CommenterTest() {
    private fun doTest(@Language("MCLang") before: String, @Language("MCLang") after: String) {
        doTest(before, after, ".lang", ProjectBuilder::lang)
    }

    @Test
    @DisplayName("Single Line Comment Test")
    fun singleLineCommentTest() = doTest(
        """
        test.<caret>key1=value1
        test.key2=value2
        """,
        """
        #test.key1=value1
        test.k<caret>ey2=value2
        """,
    )

    @Test
    @DisplayName("Multi Line Comment Test")
    fun multiLineCommentTest() = doTest(
        """
        test.key1=value1
        test.<selection>key2=value2
        test</selection>.key3=value3
        test.key4=value4
        """,
        """
        test.key1=value1
        #test.<selection>key2=value2
        #test</selection>.key3=value3
        test.key4=value4
        """,
    )

    @Test
    @DisplayName("Single Line Uncomment Test")
    fun singleLineUncommentTest() = doTest(
        """
        test.key1=value1
        test.key2=value2
        #test<caret>.key3=value3
        #test.key4=value4
        """,
        """
        test.key1=value1
        test.key2=value2
        test.key3=value3
        #tes<caret>t.key4=value4
        """,
    )

    @Test
    @DisplayName("Multi Line Uncomment Test")
    fun multiLineUncommentTest() = doTest(
        """
        #test.<selection>key1=value1
        #test.key2=</selection>value2
        #test.key3=value3
        test.key4=value4
        """,
        """
        test.<selection>key1=value1
        test.key2=</selection>value2
        #test.key3=value3
        test.key4=value4
        """,
    )
}
