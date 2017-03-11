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

import com.intellij.openapi.roots.ModuleRootModificationUtil
import com.intellij.testFramework.fixtures.LightCodeInsightFixtureTestCase

abstract class ProjectBuilderTest : LightCodeInsightFixtureTestCase() {

    protected fun buildProject(builder: ProjectBuilder.() -> Unit) = ProjectBuilder(myFixture).build(builder)

    fun ProjectBuilder.src(block: ProjectBuilder.() -> Unit) {
        dir("src", block)
        ModuleRootModificationUtil.updateModel(myFixture.module) { model ->
            val contentEntry = model.contentEntries.firstOrNull { it.file == project.baseDir } ?:
                model.addContentEntry(project.baseDir)

            val srcFolder = project.baseDir.findChild("src")!!
            if (!contentEntry.sourceFolderFiles.contains(srcFolder)) {
                contentEntry.addSourceFolder(srcFolder, false)
            }
        }
    }
}
