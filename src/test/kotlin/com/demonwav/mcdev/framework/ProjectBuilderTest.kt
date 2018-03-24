/*
 * Minecraft Dev for IntelliJ
 *
 * https://minecraftdev.org
 *
 * Copyright (c) 2018 minecraft-dev
 *
 * MIT License
 */

package com.demonwav.mcdev.framework

import com.intellij.openapi.project.rootManager
import com.intellij.openapi.roots.ModuleRootModificationUtil
import com.intellij.openapi.vfs.VfsUtil
import com.intellij.testFramework.fixtures.LightCodeInsightFixtureTestCase

abstract class ProjectBuilderTest : LightCodeInsightFixtureTestCase() {

    protected fun buildProject(builder: ProjectBuilder.() -> Unit) = ProjectBuilder(myFixture).build(builder)

    fun ProjectBuilder.src(block: ProjectBuilder.() -> Unit) {
        val srcFolder = VfsUtil.createDirectoryIfMissing(project.baseDir, "src")
        val entry = myFixture.module.rootManager.contentEntries.first { it.file == project.baseDir }
        if (!entry.sourceFolderFiles.contains(srcFolder)) {
            ModuleRootModificationUtil.updateModel(myFixture.module) { model ->
                model.contentEntries.first { it.file == project.baseDir }.addSourceFolder(srcFolder, false)
            }
        }

        dir("src", block)
    }

    override fun tearDown() {
        ModuleRootModificationUtil.updateModel(myFixture.module) { model ->
            model.removeContentEntry(model.contentEntries.first { it.file == project.baseDir })
        }

        super.tearDown()
    }
}
