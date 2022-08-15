/*
 * Minecraft Dev for IntelliJ
 *
 * https://minecraftdev.org
 *
 * Copyright (c) 2022 minecraft-dev
 *
 * MIT License
 */

package com.demonwav.mcdev.framework

import com.intellij.openapi.application.runWriteAction
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.PsiFile
import com.intellij.psi.PsiManager
import com.intellij.testFramework.fixtures.JavaCodeInsightTestFixture
import com.intellij.testFramework.fixtures.TempDirTestFixture
import com.intellij.testFramework.runInEdtAndWait
import org.intellij.lang.annotations.Language

/**
 * Like most things in this project at this point, taken from the intellij-rust folks
 * https://github.com/intellij-rust/intellij-rust/blob/master/src/test/kotlin/org/rust/ProjectBuilder.kt
 */
class ProjectBuilder(private val fixture: JavaCodeInsightTestFixture, private val tempDirFixture: TempDirTestFixture) {
    private val project
        get() = fixture.project

    var intermediatePath = ""

    fun java(
        path: String,
        @Language("JAVA") code: String,
        configure: Boolean = true,
        allowAst: Boolean = false
    ) = file(path, code, ".java", configure, allowAst)
    fun at(
        path: String,
        @Language("Access Transformers") code: String,
        configure: Boolean = true,
        allowAst: Boolean = false
    ) = file(path, code, "_at.cfg", configure, allowAst)
    fun lang(
        path: String,
        @Language("MCLang") code: String,
        configure: Boolean = true,
        allowAst: Boolean = false
    ) = file(path, code, ".lang", configure, allowAst)
    fun nbtt(
        path: String,
        @Language("NBTT") code: String,
        configure: Boolean = true,
        allowAst: Boolean = false
    ) = file(path, code, ".nbtt", configure, allowAst)

    inline fun dir(path: String, block: ProjectBuilder.() -> Unit) {
        val oldIntermediatePath = intermediatePath
        if (intermediatePath.isEmpty()) {
            intermediatePath = path
        } else {
            intermediatePath += "/$path"
        }
        block()
        intermediatePath = oldIntermediatePath
    }

    fun file(path: String, code: String, ext: String, configure: Boolean, allowAst: Boolean): VirtualFile {
        check(path.endsWith(ext))

        val fullPath = if (intermediatePath.isEmpty()) path else "$intermediatePath/$path"
        val newFile = tempDirFixture.createFile(fullPath, code.trimIndent())

        if (allowAst) {
            fixture.allowTreeAccessForFile(newFile)
        }
        if (configure) {
            fixture.configureFromExistingVirtualFile(newFile)
        }

        return newFile
    }

    fun <T : PsiFile> VirtualFile.toPsiFile(): T {
        @Suppress("UNCHECKED_CAST")
        return PsiManager.getInstance(project).findFile(this) as T
    }

    fun build(builder: ProjectBuilder.() -> Unit) {
        runInEdtAndWait {
            runWriteAction {
                builder()
            }
        }
    }
}
