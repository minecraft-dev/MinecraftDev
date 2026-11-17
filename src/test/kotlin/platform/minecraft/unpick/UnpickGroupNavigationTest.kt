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
import com.demonwav.mcdev.platform.minecraft.unpick.gen.psi.UnpickGroupDefinition
import com.demonwav.mcdev.platform.minecraft.unpick.gen.psi.UnpickGroupRef
import com.intellij.psi.PsiManager
import com.intellij.psi.util.PsiTreeUtil
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith

@ExtendWith(EdtInterceptor::class)
@DisplayName("Unpick Group Navigation Tests")
class UnpickGroupNavigationTest : ProjectBuilderTest() {

    @BeforeEach
    fun setupProject() {
        buildProject {
            unpick(
                "colors.unpick",
                """
                unpick v4

                #: A color in ARGB format.
                #: @see Colors
                group int Color
                    @format hex
                    1
                """,
                configure = false,
            )
            unpick(
                "more_colors.unpick",
                """
                unpick v4
                group int Color
                    2
                """,
                configure = false,
            )
            unpick(
                "targets.unpick",
                """
                unpick v4
                target_method foo.Bar baz (I)V
                    param 0 Co<caret>lor
                target_annotation foo.ColorInt Color
                """,
            )
        }
    }

    private fun definitions(): List<UnpickGroupDefinition> {
        val ref = PsiTreeUtil.getParentOfType(
            fixture.file.findElementAt(fixture.caretOffset),
            UnpickGroupRef::class.java,
        )!!
        return ref.multiResolve(false).map { it.element as UnpickGroupDefinition }
    }

    @Test
    @DisplayName("Resolve Across Files")
    fun resolveAcrossFiles() {
        Assertions.assertEquals(
            setOf("colors.unpick", "more_colors.unpick"),
            definitions().mapTo(HashSet()) { it.containingFile.name },
        )
    }

    @Test
    @DisplayName("Find Usages")
    fun findUsages() {
        val usages = fixture.findUsages(definitions().first())
        Assertions.assertEquals(2, usages.size)
    }

    @Test
    @DisplayName("Rename Group")
    fun renameGroup() {
        fixture.renameElementAtCaret("Colour")

        val psiManager = PsiManager.getInstance(project)
        val texts = listOf("colors.unpick", "more_colors.unpick", "targets.unpick").map { name ->
            val file = fixture.tempDirFixture.getFile(name)!!
            psiManager.findFile(file)!!.text
        }
        Assertions.assertTrue(texts.none { it.contains("Color ") || it.contains("Color\n") }, texts.toString())
        Assertions.assertEquals(2, texts[2].split("Colour").size - 1, texts[2])
        Assertions.assertTrue(texts[0].contains("group int Colour"), texts[0])
        Assertions.assertTrue(texts[1].contains("group int Colour"), texts[1])
    }

    @Test
    @DisplayName("Documentation")
    fun documentation() {
        val doc = UnpickDocumentationProvider().generateDoc(definitions().first(), null)!!
        Assertions.assertTrue(doc.contains("group int Color"), doc)
        Assertions.assertTrue(doc.contains("A color in ARGB format.<br>@see Colors"), doc)
    }

    @Test
    @DisplayName("Group Name Completion")
    fun groupNameCompletion() {
        fixture.configureByText("complete.unpick", "unpick v4\ntarget_annotation foo.Bar Co<caret>\n")
        val lookups = fixture.completeBasic()?.map { it.lookupString }
        // A single match is inserted directly
        if (lookups == null) {
            Assertions.assertTrue(fixture.editor.document.text.contains("foo.Bar Color"))
        } else {
            Assertions.assertTrue("Color" in lookups, lookups.toString())
        }
    }

    @Test
    @DisplayName("Keyword Completion")
    fun keywordCompletion() {
        fixture.configureByText("complete.unpick", "unpick v4\ngroup <caret>\n")
        val lookups = fixture.completeBasic()!!.map { it.lookupString }
        Assertions.assertEquals(
            listOf("Class", "double", "float", "int", "long", "String"),
            lookups.sortedBy { it.lowercase() },
        )
    }
}
