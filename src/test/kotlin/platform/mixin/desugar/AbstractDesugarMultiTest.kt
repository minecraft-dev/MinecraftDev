/*
 * Minecraft Development for IntelliJ
 *
 * https://mcdev.io/
 *
 * Copyright (C) 2025 minecraft-dev
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

package com.demonwav.mcdev.platform.mixin.desugar

import com.demonwav.mcdev.framework.BaseMinecraftTest
import com.demonwav.mcdev.platform.mixin.handlers.desugar.DesugarContext
import com.demonwav.mcdev.platform.mixin.handlers.desugar.DesugarUtil
import com.demonwav.mcdev.platform.mixin.handlers.desugar.Desugarer
import com.demonwav.mcdev.util.createChildPointer
import com.intellij.openapi.command.WriteCommandAction
import com.intellij.psi.PsiClass
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiJavaFile
import com.intellij.psi.PsiMethodCallExpression
import com.intellij.psi.codeStyle.CodeStyleManager
import com.intellij.psi.codeStyle.JavaCodeStyleManager
import com.intellij.psi.util.PsiTreeUtil
import com.intellij.psi.util.childrenOfType
import com.intellij.psi.util.parentOfType
import com.intellij.testFramework.IndexingTestUtil
import kotlin.collections.filterIsInstance
import org.intellij.lang.annotations.Language
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertInstanceOf
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.objectweb.asm.Opcodes

abstract class AbstractDesugarMultiTest : BaseMinecraftTest() {
    companion object {
        private const val DEFAULT_CLASS_VERSION = Opcodes.V21
    }

    abstract val desugarers: List<Desugarer>

    protected fun doTestNoChange(@Language("JAVA") code: String, classVersion: Int = DEFAULT_CLASS_VERSION) {
        doTest(code, code, classVersion)
    }

    protected fun doTest(
        @Language("JAVA") before: String,
        @Language("JAVA") after: String,
        classVersion: Int = DEFAULT_CLASS_VERSION
    ): PsiElement? {
        return WriteCommandAction.runWriteCommandAction<PsiElement?>(project) {
            val codeStyleManager = CodeStyleManager.getInstance(project)
            val javaCodeStyleManager = JavaCodeStyleManager.getInstance(project)

            val caretIndex = after.indexOf("<caret>")
            val actualAfter = if (caretIndex >= 0) {
                after.removeRange(caretIndex, caretIndex + "<caret>".length)
            } else {
                after
            }
            val expectedFile = fixture.addClass(actualAfter).containingFile
            val elementAtCaret = if (caretIndex >= 0) {
                val elementAtCaret = expectedFile.findElementAt(caretIndex)
                assertNotNull(elementAtCaret, "Could not find element at caret")
                expectedFile.createChildPointer(elementAtCaret!!)
            } else {
                null
            }
            assertEquals(
                expectedFile,
                codeStyleManager.reformat(expectedFile),
                "Reformatting changed the file!",
            )
            val expectedText = expectedFile.text
            expectedFile.delete()

            val testFile = assertInstanceOf(
                PsiJavaFile::class.java,
                fixture.configureByText("Test.java", before)
            )
            assertEquals(
                testFile,
                codeStyleManager.reformat(testFile),
                "Reformatting changed the file!",
            )

            IndexingTestUtil.waitUntilIndexesAreReady(project)

            val desugaredFile = testFile.copy() as PsiJavaFile
            DesugarUtil.setOriginalRecursive(desugaredFile, testFile)
            for (desugarer in desugarers) {
                desugarer.desugar(project, desugaredFile, DesugarContext(classVersion))
            }
            assertEquals(
                desugaredFile,
                javaCodeStyleManager.shortenClassReferences(desugaredFile),
                "Shortening class references changed the file!"
            )
            assertEquals(
                expectedText,
                codeStyleManager.reformat(desugaredFile.copy()).text
            )

            PsiTreeUtil.processElements(desugaredFile) { desugaredElement ->
                val originalElement = DesugarUtil.getOriginalElement(desugaredElement)
                if (originalElement != null) {
                    assertTrue(
                        PsiTreeUtil.isAncestor(testFile, originalElement, false)
                    ) {
                        "The original element of $desugaredElement is not from the original file"
                    }
                }
                true
            }

            val originalClasses = testFile.childrenOfType<PsiClass>()
            val desugaredClassesSet = mutableSetOf<PsiClass>()
            val originalToDesugaredMap = DesugarUtil.getOriginalToDesugaredMap(desugaredFile)
            for (clazz in originalClasses) {
                val desugaredClasses = originalToDesugaredMap[clazz]?.filterIsInstance<PsiClass>() ?: emptyList()
                assertEquals(1, desugaredClasses.size) { "Unexpected number of desugared classes for ${clazz.name}" }
                desugaredClassesSet += desugaredClasses.first()
            }
            assertEquals(originalClasses.size, desugaredClassesSet.size, "Unexpected number of desugared classes")

            elementAtCaret?.let {
                val result = elementAtCaret.dereference(desugaredFile)
                assertNotNull(result, "Could not dereference element at caret")
                result
            }
        }
    }

    protected fun doIndyTest(
        @Language("JAVA") before: String,
        @Language("JAVA") after: String,
        expectedIndyData: DesugarUtil.IndyData,
        classVersion: Int = DEFAULT_CLASS_VERSION,
    ) {
        WriteCommandAction.runWriteCommandAction(project) {
            val element = doTest(before, after, classVersion)
            assertNotNull(element, "No caret found")
            val indyCall = element!!.parentOfType<PsiMethodCallExpression>()
            assertNotNull(indyCall, "No method call found")
            val indyData = DesugarUtil.getIndyData(indyCall!!)
            assertNotNull(indyData, "Method call has no indy data")
            assertEquals(expectedIndyData, indyData)
        }
    }
}
