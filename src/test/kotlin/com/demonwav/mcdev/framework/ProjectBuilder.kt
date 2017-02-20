/*
 * Minecraft Dev for IntelliJ
 *
 * https://minecraftdev.org
 *
 * Copyright (c) 2017 minecraft-dev
 *
 * MIT License
 */

package com.demonwav.mcdev.framework

import com.intellij.openapi.application.runWriteAction
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VfsUtil
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.PsiFile
import com.intellij.psi.PsiManager
import com.intellij.testFramework.fixtures.JavaCodeInsightTestFixture
import com.intellij.util.PathUtil
import org.intellij.lang.annotations.Language

/**
 * Like most things in this project at this point, taken from the intellij-rust folks
 * https://github.com/intellij-rust/intellij-rust/blob/master/src/test/kotlin/org/rust/ProjectBuilder.kt
 */
class ProjectBuilder(
    private val fixture: JavaCodeInsightTestFixture,
    private val project: Project = fixture.project,
    private val root: VirtualFile = project.baseDir
) {
    var intermediatePath = ""

    fun java(path: String, @Language("JAVA") code: String) = file(path, code, ".java")
    fun at(path: String, @Language("Access Transformers") code: String) = file(path, code, "_at.cfg")
    fun gradle(path: String, @Language("Groovy") code: String) = file(path, code, ".gradle")
    fun xml(path: String, @Language("XML") code: String) = file(path, code, ".xml")

    fun dir(path: String, block: ProjectBuilder.() -> Unit) {
        val oldIntermediatePath = intermediatePath
        intermediatePath += "/$path"
        block()
        intermediatePath = oldIntermediatePath
    }

    private fun file(path: String, code: String, ext: String): VirtualFile {
        check(path.endsWith(ext))

        val fullPath = "$intermediatePath/$path"

        val dir = PathUtil.getParentPath(fullPath)
        val vDir = VfsUtil.createDirectoryIfMissing(root, dir)
        val vFile = vDir.findOrCreateChildData(this, PathUtil.getFileName(fullPath))
        VfsUtil.saveText(vFile, code.trimIndent())

        fixture.configureFromExistingVirtualFile(vFile)
        return vFile
    }

    fun <T : PsiFile> VirtualFile.toPsiFile(): T {
        @Suppress("UNCHECKED_CAST")
        return PsiManager.getInstance(project).findFile(this) as T
    }

    fun build(builder: ProjectBuilder.() -> Unit) {
        runWriteAction {
            VfsUtil.markDirtyAndRefresh(false, true, true, root)
            builder()
        }
    }
}
