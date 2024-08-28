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

@file:JvmName("TestUtil")

package com.demonwav.mcdev.framework

import com.intellij.ide.highlighter.JavaFileType
import com.intellij.lexer.Lexer
import com.intellij.openapi.fileTypes.FileType
import com.intellij.openapi.project.Project
import com.intellij.openapi.roots.OrderRootType
import com.intellij.openapi.roots.libraries.Library
import com.intellij.openapi.roots.libraries.LibraryTablesRegistrar
import com.intellij.openapi.util.io.FileUtil
import com.intellij.openapi.vfs.JarFileSystem
import com.intellij.openapi.vfs.StandardFileSystems
import com.intellij.openapi.vfs.VfsUtilCore
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.openapi.vfs.VirtualFileManager
import com.intellij.psi.PsiFile
import com.intellij.psi.impl.DebugUtil
import com.intellij.testFramework.LexerTestCase
import com.intellij.testFramework.fixtures.JavaCodeInsightTestFixture
import com.intellij.util.ReflectionUtil
import org.junit.jupiter.api.Assertions
import org.opentest4j.AssertionFailedError

typealias ProjectBuilderFunc =
    ProjectBuilder.(path: String, code: String, configure: Boolean, allowAst: Boolean) -> VirtualFile

const val RESOURCE_ROOT = "src/test/resources"
const val ROOT_PACKAGE = "com/demonwav/mcdev"
const val BASE_DATA_PATH = "$RESOURCE_ROOT/$ROOT_PACKAGE"
const val BASE_DATA_PATH_2 = "\$PROJECT_ROOT/$BASE_DATA_PATH/"

val mockJdk by lazy {
    val path = findLibraryPath("mock-jdk")
    val rtFile = StandardFileSystems.local().findFileByPath(path)!!
    val rt = JarFileSystem.getInstance().getRootByLocal(rtFile)!!
    val home = rtFile.parent!!
    MockJdk("1.7", rt, home)
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

fun testInspectionFix(
    fixture: JavaCodeInsightTestFixture,
    fixName: String,
    fileType: FileType,
    before: String,
    after: String
) {
    fixture.configureByText(fileType, before)
    val intention = fixture.findSingleIntention(fixName)
    fixture.launchAction(intention)
    fixture.checkResult(after)
}

fun <T> assertEqualsUnordered(expected: Collection<T>, actual: Collection<T>) {
    val expectedSet = expected.toSet()
    val actualSet = actual.toSet()
    val notFound = expectedSet.minus(actualSet)
    val notExpected = actualSet.minus(expectedSet)

    if (notExpected.isNotEmpty() && notFound.isNotEmpty()) {
        val message = """|
      |Expecting actual:
      |  $actual
      |to contain exactly in any order:
      |  $expected
      |elements not found:
      |  $notFound
      |and elements not expected:
      |  $notExpected
    """.trimMargin()
        throw AssertionFailedError(message, expected, actual)
    }
    if (notFound.isNotEmpty()) {
        val message = """|
      |Expecting actual:
      |  $actual
      |to contain exactly in any order:
      |  $expected
      |but could not find the following elements:
      |  $notFound
    """.trimMargin()
        throw AssertionFailedError(message, expected, actual)
    }
    if (notExpected.isNotEmpty()) {
        val message = """|
      |Expecting actual:
      |  $actual
      |to contain exactly in any order:
      |  $expected
      |but the following elements were unexpected:
      |  $notExpected
    """.trimMargin()
        throw AssertionFailedError(message, expected, actual)
    }
}
