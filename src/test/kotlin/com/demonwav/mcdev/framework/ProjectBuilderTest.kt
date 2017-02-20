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
            model.addContentEntry(project.baseDir).addSourceFolder(project.baseDir.findChild("src")!!, false)
        }
    }
}
