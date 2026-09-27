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

@file:JvmName("TestUtil")

package com.demonwav.mcdev.framework

import com.intellij.ide.highlighter.JavaFileType
import com.intellij.lexer.Lexer
import com.intellij.openapi.project.Project
import com.intellij.openapi.roots.OrderRootType
import com.intellij.openapi.roots.libraries.Library
import com.intellij.openapi.roots.libraries.LibraryTablesRegistrar
import com.intellij.openapi.util.TextRange
import com.intellij.openapi.util.io.FileUtil
import com.intellij.openapi.vfs.JarFileSystem
import com.intellij.openapi.vfs.StandardFileSystems
import com.intellij.openapi.vfs.VfsUtilCore
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.openapi.vfs.VirtualFileManager
import com.intellij.psi.PsiFile
import com.intellij.psi.impl.DebugUtil
import com.intellij.testFramework.IdeaTestUtil
import com.intellij.testFramework.LexerTestCase
import com.intellij.testFramework.fixtures.JavaCodeInsightTestFixture
import com.intellij.util.ReflectionUtil
import org.junit.jupiter.api.Assertions

typealias ProjectBuilderFunc =
    ProjectBuilder.(path: String, code: String, configure: Boolean, allowAst: Boolean) -> VirtualFile

const val RESOURCE_ROOT = "src/test/resources"
const val ROOT_PACKAGE = "com/demonwav/mcdev"
const val BASE_DATA_PATH = "$RESOURCE_ROOT/$ROOT_PACKAGE"
const val BASE_DATA_PATH_2 = "\$PROJECT_ROOT/$BASE_DATA_PATH/"

val mockJdk by lazy {
    IdeaTestUtil.getMockJdk21()
}

fun findLibraryPath(name: String) = FileUtil.toSystemIndependentName(System.getProperty("testLibs.$name")!!)
private fun findLibrary(name: String): VirtualFile? {
    val fsRoot = StandardFileSystems.jar()
        .refreshAndFindFileByPath(findLibraryPath(name) + JarFileSystem.JAR_SEPARATOR)
    if (fsRoot != null) {
        // force refresh every directory, it's the only way I could get intellij to behave
        VfsUtilCore.iterateChildrenRecursively(
            fsRoot,
            { it.isDirectory },
            {
                it.children
                it.refresh(false, false)
                true
            },
        )
    }
    return fsRoot
}

fun createLibrary(project: Project, name: String): Library {
    val table = LibraryTablesRegistrar.getInstance().getLibraryTable(project)
    return table.getLibraryByName(name) ?: run {
        val library = table.createLibrary(name)
        val libraryModel = library.modifiableModel
        libraryModel.addRoot(findLibrary(name)!!, OrderRootType.CLASSES)
        libraryModel.commit()
        // sync refresh, otherwise intellij might not find files added to the library jar
        VirtualFileManager.getInstance().syncRefresh()
        library
    }
}

fun String.filterCrlf() = this.filter { it != '\r' }

fun testLexer(basePath: String, lexer: Lexer) {
    val caller = ReflectionUtil.getCallerClass(3)!!
    val text = caller.getResource(basePath)!!.readText().trim()

    val expected = caller.getResource("${basePath.substringBeforeLast('.')}.txt")?.readText()?.trim()
        ?: Assertions.fail("no test data")
    val actual = LexerTestCase.printTokens(text.filterCrlf(), 0, lexer).trim()

    Assertions.assertEquals(expected.filterCrlf(), actual.filterCrlf())
}

fun ProjectBuilderTest.testParser(basePath: String, func: ProjectBuilderFunc) {
    val caller = ReflectionUtil.getCallerClass(3)!!
    val text = caller.getResource(basePath)?.readText()?.trim() ?: Assertions.fail("no test data")
    val expected = caller.getResource("${basePath.substringBeforeLast('.')}.txt")?.readText()?.trim()
        ?: Assertions.fail("no expected data")

    var file: PsiFile? = null
    buildProject {
        file = func(basePath.substringAfterLast('/'), text, true, false).toPsiFile()
    }

    val actual = DebugUtil.psiToString(file!!, true, true).trim()

    Assertions.assertEquals(expected.filterCrlf(), actual.filterCrlf())
}

fun testInspectionFix(fixture: JavaCodeInsightTestFixture, basePath: String, fixName: String) {
    val caller = ReflectionUtil.getCallerClass(4)!!
    val original =
        caller.getResource("$basePath.java")?.readText()?.trim()?.lineSequence()?.joinToString("\n")
            ?: Assertions.fail("no test data")
    val expected = caller.getResource("$basePath.after.java")?.readText()?.trim()?.lineSequence()
        ?.joinToString("\n") ?: Assertions.fail("no expected data")

    fixture.configureByText(JavaFileType.INSTANCE, original)
    val intention = fixture.findSingleIntention(fixName)
    fixture.launchAction(intention)
    fixture.checkResult(expected)
}

fun testAllInspectionFixes(fixture: JavaCodeInsightTestFixture, basePath: String, fixName: String) {
    val caller = ReflectionUtil.getCallerClass(4)!!
    val original =
        caller.getResource("$basePath.java")?.readText()?.trim()?.lineSequence()?.joinToString("\n")
            ?: Assertions.fail("no test data")
    val expected = caller.getResource("$basePath.after.java")?.readText()?.trim()?.lineSequence()
        ?.joinToString("\n") ?: Assertions.fail("no expected data")

    fixture.configureByText(JavaFileType.INSTANCE, original)

    var lastFixedRange: TextRange? = null
    while (true) {
        val (fix, range) = fixture.doHighlighting().firstNotNullOfOrNull { info ->
            info.findRegisteredQuickFix { descriptor, range ->
                descriptor.action.takeIf { it.familyName == fixName }?.let { it to range }
            }
        } ?: break

        Assertions.assertNotEquals(lastFixedRange, range, "Fix loop")
        lastFixedRange = range

        fixture.launchAction(fix)
    }

    Assertions.assertTrue(lastFixedRange != null, "No fixes applied")

    fixture.checkResult(expected)
}
