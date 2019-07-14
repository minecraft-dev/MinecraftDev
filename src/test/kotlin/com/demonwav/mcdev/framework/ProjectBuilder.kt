/*
 * Minecraft Dev for IntelliJ
 *
 * https://minecraftdev.org
 *
 * Copyright (c) 2019 minecraft-dev
 *
 * MIT License
 */

package com.demonwav.mcdev.framework

import com.demonwav.mcdev.i18n.I18nConstants
import com.intellij.openapi.application.runWriteAction
import com.intellij.openapi.project.rootManager
import com.intellij.openapi.roots.ModuleRootModificationUtil
import com.intellij.openapi.vfs.VfsUtil
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.PsiFile
import com.intellij.psi.PsiManager
import com.intellij.testFramework.fixtures.JavaCodeInsightTestFixture
import com.intellij.testFramework.runInEdtAndWait
import com.intellij.util.PathUtil
import org.intellij.lang.annotations.Language
import java.lang.ref.WeakReference

/**
 * Like most things in this project at this point, taken from the intellij-rust folks
 * https://github.com/intellij-rust/intellij-rust/blob/master/src/test/kotlin/org/rust/ProjectBuilder.kt
 */
class ProjectBuilder(fixture: JavaCodeInsightTestFixture, private val root: VirtualFile) {
    private val fixtureRef = WeakReference(fixture)

    private val fixture: JavaCodeInsightTestFixture
        get() {
            val fix = fixtureRef.get() ?: throw Exception("Reference collected")
            if (fix.project.isDisposed) {
                throw Exception("Project disposed")
            }
            return fix
        }
    private val project
        get() = fixture.project

    var intermediatePath = ""

    fun java(path: String, @Language("JAVA") code: String, configure: Boolean = true) =
        file(path, code, ".java", configure)
    fun at(path: String, @Language("Access Transformers") code: String, configure: Boolean = true) =
        file(path, code, "_at.cfg", configure)
    fun i18n(path: String, @Language("I18n") code: String, configure: Boolean = true) =
        file(path, code, ".${I18nConstants.FILE_EXTENSION}", configure)
    fun nbtt(path: String, @Language("NBTT") code: String, configure: Boolean = true) =
        file(path, code, ".nbtt", configure)

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

    fun file(path: String, code: String, ext: String, configure: Boolean): VirtualFile {
        check(path.endsWith(ext))

        val fullPath = if (intermediatePath.isEmpty()) path else "$intermediatePath/$path"

        val dir = PathUtil.getParentPath(fullPath)
        val vDir = VfsUtil.createDirectoryIfMissing(root, dir)
        val vFile = vDir.findOrCreateChildData(this, PathUtil.getFileName(fullPath))
        VfsUtil.saveText(vFile, code.trimIndent())

        if (configure) {
            fixture.configureFromExistingVirtualFile(vFile)
        }
        return vFile
    }

    fun <T : PsiFile> VirtualFile.toPsiFile(): T {
        @Suppress("UNCHECKED_CAST")
        return PsiManager.getInstance(project).findFile(this) as T
    }

    fun build(builder: ProjectBuilder.() -> Unit) {
        runInEdtAndWait {
            runWriteAction {
                VfsUtil.markDirtyAndRefresh(false, true, true, root)
                // Make sure to always add the module content root
                if (fixture.module.rootManager.contentEntries.none { it.file == project.baseDirPath }) {
                    ModuleRootModificationUtil.addContentRoot(fixture.module, project.baseDirPath)
                }

                builder()
            }
        }
    }
}
