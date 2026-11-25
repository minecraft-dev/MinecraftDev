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

import com.demonwav.mcdev.framework.BaseMinecraftTest
import com.demonwav.mcdev.framework.EdtInterceptor
import com.intellij.psi.JavaPsiFacade
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.psi.search.searches.ReferencesSearch
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith

@ExtendWith(EdtInterceptor::class)
@DisplayName("Unpick Class Reference Tests")
class UnpickClassReferenceTest : BaseMinecraftTest() {

    @BeforeEach
    fun setupProject() {
        buildProject {
            java(
                "com/example/Outer.java",
                """
                package com.example;

                public class Outer {
                    public static final int OUTER_VALUE = 1;

                    public static class Middle {
                        public static class Inner {
                            public static final int INNER_VALUE = 2;
                        }
                    }

                    public void method() {
                        class Local {
                        }
                        Runnable r = new Runnable() {
                            public void run() {
                            }
                        };
                    }
                }
                """,
                configure = false,
                allowAst = true,
            )
            unpick(
                "usages.unpick",
                """
                unpick v4
                group int
                    com.example.Outer${'$'}Middle${'$'}Inner.INNER_VALUE
                    com.example.Outer.OUTER_VALUE
                target_field com.example.Outer${'$'}Middle${'$'}Inner INNER_VALUE I Foo
                """,
                configure = false,
            )
        }
    }

    private fun findClass(name: String) =
        JavaPsiFacade.getInstance(project).findClass(name, GlobalSearchScope.allScope(project))!!

    private fun usagesInUnpick(className: String): List<String> {
        return ReferencesSearch.search(findClass(className)).findAll()
            .filter { it.element.containingFile is UnpickFile }
            .map { it.canonicalText }
    }

    @Test
    @DisplayName("Find Usages Of Inner Class")
    fun findUsagesOfInnerClass() {
        Assertions.assertEquals(
            listOf("com.example.Outer\$Middle\$Inner", "com.example.Outer\$Middle\$Inner"),
            usagesInUnpick("com.example.Outer.Middle.Inner"),
        )
    }

    @Test
    @DisplayName("Find Usages Of Middle Class")
    fun findUsagesOfMiddleClass() {
        Assertions.assertEquals(
            listOf("com.example.Outer\$Middle", "com.example.Outer\$Middle"),
            usagesInUnpick("com.example.Outer.Middle"),
        )
    }

    @Test
    @DisplayName("Find Usages Of Outer Class")
    fun findUsagesOfOuterClass() {
        Assertions.assertEquals(3, usagesInUnpick("com.example.Outer").size)
    }

    @Test
    @DisplayName("Rename Inner Class")
    fun renameInnerClass() {
        fixture.renameElement(findClass("com.example.Outer.Middle.Inner"), "Renamed")
        val text = fixture.tempDirFixture.getFile("usages.unpick")!!.let { fixture.psiManager.findFile(it)!!.text }
        Assertions.assertEquals(2, text.split("Outer\$Middle\$Renamed").size - 1, text)
    }

    private fun complete(text: String): List<String> {
        fixture.configureByText("complete.unpick", text)
        return fixture.completeBasic()?.map { it.lookupString }.orEmpty()
    }

    @Test
    @DisplayName("Inner Class Completion Includes Local And Anonymous Classes")
    fun innerClassCompletion() {
        val lookups = complete("unpick v4\ntarget_annotation com.example.Outer${'$'}<caret>\n")
        Assertions.assertTrue("Middle" in lookups, lookups.toString())
        Assertions.assertTrue("1Local" in lookups, lookups.toString())
        Assertions.assertTrue("1" in lookups, lookups.toString())
    }

    @Test
    @DisplayName("Field Expression Owner Completion")
    fun fieldExpressionOwnerCompletion() {
        val lookups = complete("unpick v4\ngroup int\n    com.example.Ou<caret>\n")
        Assertions.assertTrue(lookups.isEmpty() || "Outer" in lookups, lookups.toString())
        if (lookups.isEmpty()) {
            Assertions.assertTrue(fixture.editor.document.text.contains("com.example.Outer"))
        }
    }

    @Test
    @DisplayName("Field Expression Field Completion")
    fun fieldExpressionFieldCompletion() {
        val lookups = complete("unpick v4\ngroup int\n    com.example.Outer.OUT<caret>\n")
        if (lookups.isEmpty()) {
            Assertions.assertTrue(fixture.editor.document.text.contains("Outer.OUTER_VALUE"))
        } else {
            Assertions.assertTrue("OUTER_VALUE" in lookups, lookups.toString())
        }
    }

    @Test
    @DisplayName("Simple Name Class Completion")
    fun simpleNameClassCompletion() {
        complete("unpick v4\ntarget_annotation Oute<caret>\n")
        val text = fixture.editor.document.text
        if (!text.contains("com.example.Outer")) {
            fixture.lookup?.let { lookup ->
                val item = lookup.items.first { it.lookupString == "com.example.Outer" || it.lookupString == "Outer" }
                fixture.lookup.currentItem = item
                fixture.finishLookup('\n')
            }
        }
        Assertions.assertTrue(
            fixture.editor.document.text.contains("target_annotation com.example.Outer"),
            fixture.editor.document.text,
        )
    }

    @Test
    @DisplayName("Simple Name Field Expression Completion")
    fun simpleNameFieldExpressionCompletion() {
        complete("unpick v4\ngroup int\n    Inne<caret>\n")
        val text = fixture.editor.document.text
        if (!text.contains("com.example.Outer\$Middle\$Inner")) {
            fixture.lookup?.let { lookup ->
                val item = lookup.items.first { it.lookupString.endsWith("Inner") }
                fixture.lookup.currentItem = item
                fixture.finishLookup('\n')
            }
        }
        Assertions.assertTrue(
            fixture.editor.document.text.contains("    com.example.Outer\$Middle\$Inner"),
            fixture.editor.document.text,
        )
    }

    @Test
    @DisplayName("Completion After Hash In String")
    fun completionAfterHashInString() {
        complete("unpick v4\ngroup String\n    \"a#b\" + Inne<caret>\n")
        if (!fixture.editor.document.text.contains("com.example.Outer\$Middle\$Inner")) {
            fixture.lookup?.let { lookup ->
                fixture.lookup.currentItem = lookup.items.first { it.lookupString.endsWith("Inner") }
                fixture.finishLookup('\n')
            }
        }
        Assertions.assertTrue(
            fixture.editor.document.text.contains("\"a#b\" + com.example.Outer\$Middle\$Inner"),
            fixture.editor.document.text,
        )
    }

    @Test
    @DisplayName("No Completion In Comments And Literals")
    fun noCompletionInCommentsAndLiterals() {
        for (line in listOf("1 # Inne<caret>", "\"Inne<caret>\"")) {
            val text = "unpick v4\ngroup String\n    $line\n"
            val lookups = complete(text)
            Assertions.assertEquals(emptyList<String>(), lookups)
            Assertions.assertEquals(text.replace("<caret>", ""), fixture.editor.document.text)
        }
    }
}
